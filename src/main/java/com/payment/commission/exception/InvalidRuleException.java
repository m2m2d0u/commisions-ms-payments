package com.payment.commission.exception;

/**
 * Exception thrown when a commission rule is invalid
 */
public class InvalidRuleException extends RuntimeException {

    public InvalidRuleException(String message) {
        super(message);
    }

    public InvalidRuleException(String message, Throwable cause) {
        super(message, cause);
    }
}
