package com.payment.commission.exception;

/**
 * Exception thrown when no matching commission rule is found for transaction criteria
 */
public class NoMatchingRuleException extends RuntimeException {

    public NoMatchingRuleException(String message) {
        super(message);
    }

    public NoMatchingRuleException(String message, Throwable cause) {
        super(message, cause);
    }
}
