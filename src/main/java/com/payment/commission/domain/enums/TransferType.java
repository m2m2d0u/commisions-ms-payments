package com.payment.commission.domain.enums;

/**
 * Transfer type enumeration for commission rules.
 *
 * SAME_WALLET: Transfer within the same provider (e.g., Orange Money → Orange Money)
 * CROSS_WALLET: Transfer between different providers (e.g., Orange Money → Wave)
 * INTERNATIONAL: Cross-country or cross-currency transfers
 */
public enum TransferType {
    SAME_WALLET,      // Same provider (Orange → Orange)
    CROSS_WALLET,     // Different providers (Orange → Wave)
    INTERNATIONAL     // Cross-country transfer
}
