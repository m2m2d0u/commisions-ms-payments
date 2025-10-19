package com.payment.commission.controller.advice;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.payment.commission.exception.*;
import com.payment.commission.service.MessageService;
import com.payment.common.dto.ErrorResponse;
import com.payment.common.dto.ValidationError;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Global Exception Handler for all controllers
 * Uses shared ErrorResponse and ValidationError from common library
 * Supports i18n via MessageService
 */
@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageService messageService;

    /**
     * Handle rule not found exception
     */
    @ExceptionHandler(RuleNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRuleNotFound(
            RuleNotFoundException ex,
            HttpServletRequest request) {

        log.error("Rule not found: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.of(
            ErrorCodes.RULE_NOT_FOUND,
            ex.getMessage(),
            request.getRequestURI()
        );

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(error);
    }

    /**
     * Handle invalid rule exception
     */
    @ExceptionHandler(InvalidRuleException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRule(
            InvalidRuleException ex,
            HttpServletRequest request) {

        log.error("Invalid rule: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.of(
            ErrorCodes.INVALID_RULE,
            ex.getMessage(),
            request.getRequestURI()
        );

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(error);
    }

    /**
     * Handle no matching rule exception
     */
    @ExceptionHandler(NoMatchingRuleException.class)
    public ResponseEntity<ErrorResponse> handleNoMatchingRule(
            NoMatchingRuleException ex,
            HttpServletRequest request) {

        log.error("No matching rule: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.of(
            ErrorCodes.NO_MATCHING_RULE,
            ex.getMessage(),
            request.getRequestURI()
        );

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(error);
    }

    /**
     * Handle method argument validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        // Convert Spring validation errors to ValidationError objects
        List<ValidationError> validationErrors = ex.getBindingResult()
            .getAllErrors()
            .stream()
            .map(error -> {
                String fieldName = ((FieldError) error).getField();
                Object rejectedValue = ((FieldError) error).getRejectedValue();
                String errorMessage = error.getDefaultMessage();

                // Try to translate the error message if it's a message key
                String translatedMessage = messageService.getMessageOrDefault(errorMessage, errorMessage);

                return ValidationError.of(fieldName, rejectedValue, translatedMessage);
            })
            .collect(Collectors.toList());

        log.error("Validation errors: {}", validationErrors);

        String message = messageService.getMessage("error.validation.failed");

        ErrorResponse error = ErrorResponse.withValidationErrors(
            ErrorCodes.VALIDATION_ERROR,
            message,
            validationErrors
        );
        error.setPath(request.getRequestURI());

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(error);
    }

    /**
     * Handle JSON parsing errors (malformed JSON, type mismatches, etc.)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        log.error("JSON parse error: {}", ex.getMessage());

        List<ValidationError> validationErrors = new ArrayList<>();
        String message = messageService.getMessage("error.validation.failed");

        // Handle InvalidFormatException (type mismatch, enum errors, date format, etc.)
        if (ex.getCause() instanceof InvalidFormatException) {
            InvalidFormatException cause = (InvalidFormatException) ex.getCause();

            // Extract field name from JSON path
            String fieldName = cause.getPath().isEmpty() ? "unknown"
                             : cause.getPath().get(0).getFieldName();

            // Get the invalid value that was provided
            Object rejectedValue = cause.getValue();

            // Get the target type (Currency, TransferType, KYCLevel, LocalDateTime, etc.)
            Class<?> targetType = cause.getTargetType();

            String errorMessage;

            // Handle enum types (Currency, TransferType, KYCLevel, CommissionStatus, etc.)
            if (targetType.isEnum()) {
                Object[] enumConstants = targetType.getEnumConstants();
                String enumValues = Arrays.stream(enumConstants)
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));

                errorMessage = messageService.getMessage("error.json.enum.invalid", fieldName, enumValues);
            }
            // Handle date/time types
            else if (targetType.getSimpleName().contains("LocalDate") ||
                     targetType.getSimpleName().contains("LocalDateTime") ||
                     targetType.getSimpleName().contains("Instant")) {
                errorMessage = messageService.getMessage("error.json.date.invalid", fieldName);
            }
            // Handle UUID
            else if (targetType.getSimpleName().equals("UUID")) {
                errorMessage = messageService.getMessage("error.json.uuid.invalid", fieldName);
            }
            // Handle numeric types (amount, percentage, etc.)
            else if (Number.class.isAssignableFrom(targetType) ||
                     targetType.isPrimitive()) {
                errorMessage = messageService.getMessage("error.json.numeric.invalid", fieldName);
            }
            // Handle BigDecimal (for percentage fees)
            else if (targetType.getSimpleName().equals("BigDecimal")) {
                errorMessage = messageService.getMessage("error.json.decimal.invalid", fieldName);
            }
            // Generic type mismatch
            else {
                errorMessage = messageService.getMessage("error.json.type.invalid",
                                                        fieldName, targetType.getSimpleName());
            }

            validationErrors.add(ValidationError.of(fieldName, rejectedValue, errorMessage));
        }
        // Handle MismatchedInputException (missing required fields, null values, etc.)
        else if (ex.getCause() instanceof MismatchedInputException) {
            MismatchedInputException cause = (MismatchedInputException) ex.getCause();

            String fieldName = cause.getPath().isEmpty() ? "unknown"
                             : cause.getPath().get(0).getFieldName();

            String errorMessage = messageService.getMessage("error.json.missing.value", fieldName);
            validationErrors.add(ValidationError.of(fieldName, null, errorMessage));
        }
        // Generic JSON parse error
        else {
            message = messageService.getMessage("error.json.malformed");
        }

        ErrorResponse error;
        if (!validationErrors.isEmpty()) {
            error = ErrorResponse.withValidationErrors(
                ErrorCodes.VALIDATION_ERROR,
                message,
                validationErrors
            );
        } else {
            error = ErrorResponse.of(
                ErrorCodes.VALIDATION_ERROR,
                message,
                request.getRequestURI()
            );
        }

        error.setPath(request.getRequestURI());

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(error);
    }

    /**
     * Handle all other unexpected exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unexpected error occurred", ex);

        String message = messageService.getMessage("error.internal");

        ErrorResponse error = ErrorResponse.of(
            ErrorCodes.INTERNAL_ERROR,
            message,
            request.getRequestURI()
        );

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(error);
    }
}
