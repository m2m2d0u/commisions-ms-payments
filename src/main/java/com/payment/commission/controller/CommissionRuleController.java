package com.payment.commission.controller;

import com.payment.commission.dto.request.CreateRuleRequest;
import com.payment.commission.dto.request.UpdateRuleRequest;
import com.payment.commission.dto.response.CommissionRuleResponse;
import com.payment.commission.service.CommissionRuleService;
import com.payment.commission.service.MessageService;
import com.payment.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for Commission Rule management
 */
@RestController
@RequestMapping("/api/v1/commissions/rules")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Commission Rules", description = "Commission rule management APIs")
public class CommissionRuleController {

    private final CommissionRuleService commissionRuleService;
    private final MessageService messageService;

    /**
     * Create a new commission rule
     */
    @PostMapping
    @Operation(summary = "Create commission rule", description = "Create a new commission rule (Admin only)")
    public ResponseEntity<ApiResponse<CommissionRuleResponse>> createRule(
            @Valid @RequestBody CreateRuleRequest request,
            @RequestHeader(value = "X-User-ID", required = false) UUID userId) {
        log.info("Creating commission rule for provider: {}", request.getProviderId());

        CommissionRuleResponse response = commissionRuleService.createRule(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse.success(messageService.getMessage("success.rule.created"), response)
        );
    }

    /**
     * Update an existing commission rule
     */
    @PutMapping("/{ruleId}")
    @Operation(summary = "Update commission rule", description = "Update an existing commission rule (Admin only)")
    public ResponseEntity<ApiResponse<CommissionRuleResponse>> updateRule(
            @PathVariable UUID ruleId,
            @Valid @RequestBody UpdateRuleRequest request) {
        log.info("Updating commission rule: {}", ruleId);

        CommissionRuleResponse response = commissionRuleService.updateRule(ruleId, request);

        return ResponseEntity.ok(
            ApiResponse.success(messageService.getMessage("success.rule.updated"), response)
        );
    }

    /**
     * Get commission rule by ID
     */
    @GetMapping("/{ruleId}")
    @Operation(summary = "Get commission rule", description = "Get commission rule by ID")
    public ResponseEntity<ApiResponse<CommissionRuleResponse>> getRuleById(@PathVariable UUID ruleId) {
        log.info("Fetching commission rule: {}", ruleId);

        CommissionRuleResponse response = commissionRuleService.getRuleById(ruleId);

        return ResponseEntity.ok(
            ApiResponse.success(messageService.getMessage("success.rule.retrieved"), response)
        );
    }

    /**
     * Get all commission rules (paginated)
     */
    @GetMapping
    @Operation(summary = "Get all commission rules", description = "Get all commission rules with pagination and filtering")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllRules(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID providerId) {
        log.info("Fetching commission rules - page: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<CommissionRuleResponse> rules;

        if (providerId != null) {
            rules = commissionRuleService.getRulesByProvider(providerId, pageable);
        } else {
            rules = commissionRuleService.getAllRules(pageable);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("content", rules.getContent());
        data.put("page", rules.getNumber());
        data.put("size", rules.getSize());
        data.put("totalElements", rules.getTotalElements());
        data.put("totalPages", rules.getTotalPages());

        return ResponseEntity.ok(
            ApiResponse.success(messageService.getMessage("success.rules.retrieved"), data)
        );
    }

    /**
     * Deactivate a commission rule
     */
    @DeleteMapping("/{ruleId}")
    @Operation(summary = "Deactivate commission rule", description = "Deactivate a commission rule (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deactivateRule(@PathVariable UUID ruleId) {
        log.info("Deactivating commission rule: {}", ruleId);

        commissionRuleService.deactivateRule(ruleId);

        return ResponseEntity.ok(
            ApiResponse.success(messageService.getMessage("success.rule.deleted"))
        );
    }

    /**
     * Activate a commission rule
     */
    @PatchMapping("/{ruleId}/activate")
    @Operation(summary = "Activate commission rule", description = "Activate a deactivated commission rule")
    public ResponseEntity<ApiResponse<Void>> activateRule(@PathVariable UUID ruleId) {
        log.info("Activating commission rule: {}", ruleId);

        commissionRuleService.activateRule(ruleId);

        return ResponseEntity.ok(
            ApiResponse.success(messageService.getMessage("success.rule.activated"))
        );
    }
}
