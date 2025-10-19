package com.payment.commission.domain.enums;

/**
 * Commission transaction status.
 *
 * PENDING: Commission is pending (transaction not yet completed)
 * COMPLETED: Commission collected successfully
 * REFUNDED: Commission refunded (transaction reversed)
 */
public enum CommissionStatus {
    PENDING,      // Commission pending
    COMPLETED,    // Commission collected
    REFUNDED      // Commission refunded (transaction reversed)
}
