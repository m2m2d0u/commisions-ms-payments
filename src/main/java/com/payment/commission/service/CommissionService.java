package com.payment.commission.service;

import com.payment.common.enums.Currency;
import com.payment.common.enums.KYCLevel;
import com.payment.common.enums.TransferType;
import com.payment.common.dto.commission.request.CalculateFeeRequest;
import com.payment.common.dto.commission.response.FeeCalculationResponse;

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
     * Record commission for a completed transaction
     */
    void recordCommission(UUID transactionId, UUID ruleId,
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
