package com.payment.commission.domain.enums;

/**
 * Settlement status for commission transactions.
 *
 * PENDING: Settlement not yet processed
 * SETTLED: Settlement completed with provider
 */
public enum SettlementStatus {
    PENDING,      // Settlement pending
    SETTLED       // Settlement completed
}
