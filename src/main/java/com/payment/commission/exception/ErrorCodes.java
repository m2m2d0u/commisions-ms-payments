package com.payment.commission.exception;

/**
 * Standard error codes for Commission Service
 */
public final class ErrorCodes {

    // Validation errors (1xxx)
    public static final String VALIDATION_ERROR = "ERR_1000";
    public static final String INVALID_AMOUNT = "ERR_1001";
    public static final String INVALID_CURRENCY = "ERR_1002";
    public static final String INVALID_TRANSFER_TYPE = "ERR_1003";
    public static final String INVALID_RULE_CONFIGURATION = "ERR_1004";

    // Resource errors (2xxx)
    public static final String RESOURCE_NOT_FOUND = "ERR_2000";
    public static final String RULE_NOT_FOUND = "ERR_2001";
    public static final String COMMISSION_NOT_FOUND = "ERR_2002";
    public static final String PROVIDER_NOT_FOUND = "ERR_2003";

    // Business rule errors (3xxx)
    public static final String BUSINESS_RULE_VIOLATION = "ERR_3000";
    public static final String NO_MATCHING_RULE = "ERR_3001";
    public static final String INVALID_RULE = "ERR_3002";
    public static final String RULE_CONFLICT = "ERR_3003";
    public static final String INVALID_DATE_RANGE = "ERR_3004";

    // Internal errors (5xxx)
    public static final String INTERNAL_ERROR = "ERR_5000";
    public static final String DATABASE_ERROR = "ERR_5001";
    public static final String CACHE_ERROR = "ERR_5002";

    private ErrorCodes() {
        throw new UnsupportedOperationException("Utility class");
    }
}
