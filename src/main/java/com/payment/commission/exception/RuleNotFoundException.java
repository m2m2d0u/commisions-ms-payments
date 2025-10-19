package com.payment.commission.exception;

import java.util.UUID;

/**
 * Exception thrown when a commission rule is not found
 */
public class RuleNotFoundException extends RuntimeException {

    public RuleNotFoundException(UUID ruleId) {
        super(String.format("Règle de commission non trouvée: %s", ruleId));
    }

    public RuleNotFoundException(String message) {
        super(message);
    }
}
