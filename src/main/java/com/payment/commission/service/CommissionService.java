package com.payment.commission.service;

import com.payment.common.enums.Currency;
import com.payment.common.enums.KYCLevel;
import com.payment.commission.domain.enums.TransferType;
import com.payment.commission.dto.request.CalculateFeeRequest;
import com.payment.commission.dto.response.FeeCalculationResponse;

import java.util.UUID;

/**
 * Commission Service Interface
 * Handles fee calculation and commission recording
 */
public interface CommissionService {

    /**
     * Calculate fee for a transaction
     */
    FeeCalculationResponse calculateFee(CalculateFeeRequest request);

    /**
     * Calculate fee amount
     */
    Long calculateFee(Long amount, Currency currency, UUID providerId,
                     TransferType transferType, KYCLevel kycLevel);

    /**
     * Record commission for a completed transaction
     */
    void recordCommission(UUID transactionId, UUID ruleId, UUID providerId,
                         Long amount, Currency currency, String calculationBasis);

    /**
     * Calculate BCEAO-compliant fee
     */
    Long calculateBCEAOFee(Long amount);

    /**
     * Mark commission as refunded
     */
    void refundCommission(UUID transactionId);
}
