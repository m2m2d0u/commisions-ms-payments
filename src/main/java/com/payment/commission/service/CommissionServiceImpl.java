package com.payment.commission.service;

import com.payment.commission.domain.entity.CommissionRule;
import com.payment.commission.domain.entity.CommissionTransaction;
import com.payment.commission.domain.enums.CommissionStatus;
import com.payment.common.enums.Currency;
import com.payment.common.enums.KYCLevel;
import com.payment.commission.domain.enums.TransferType;
import com.payment.commission.dto.request.CalculateFeeRequest;
import com.payment.commission.dto.response.FeeCalculationResponse;
import com.payment.commission.repository.CommissionTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.payment.common.i18n.MessageService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Commission Service Implementation
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CommissionServiceImpl implements CommissionService {

    private final FeeCalculationEngine feeCalculationEngine;
    private final CommissionTransactionRepository commissionTransactionRepository;
    private final CommissionEventPublisher eventPublisher;
    private final MessageService messageService;

    @Override
    @Transactional(readOnly = true)
    public FeeCalculationResponse calculateFee(CalculateFeeRequest request) {
        log.info("Calculating fee for request with ruleId: {}", request.getRuleId());

        // Get the specified commission rule
        CommissionRule rule = feeCalculationEngine.getRuleById(request.getRuleId());

        // Calculate fee using the specified rule
        Long feeAmount = feeCalculationEngine.calculateFeeByRuleId(
                request.getRuleId(),
                request.getAmount()
        );

        // Build calculation details with rule information
        Map<String, Object> calculationDetails = new HashMap<>();
        calculationDetails.put("ruleId", rule.getRuleId());
        calculationDetails.put("transferType", rule.getTransferType());
        calculationDetails.put("percentage", rule.getPercentage());
        calculationDetails.put("fixedAmount", rule.getFixedAmount());
        calculationDetails.put("minAmount", rule.getMinAmount());
        calculationDetails.put("maxAmount", rule.getMaxAmount());
        calculationDetails.put("priority", rule.getPriority());
        calculationDetails.put("kycLevel", rule.getKycLevel());
        calculationDetails.put("finalAmount", feeAmount);
        calculationDetails.put("ruleDescription", rule.getDescription());
        calculationDetails.put("requestedAmount", request.getAmount());
        calculationDetails.put("requestedCurrency", request.getCurrency());
        calculationDetails.put("requestedTransferType", request.getTransferType());

        return FeeCalculationResponse.builder()
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .commissionAmount(feeAmount)
                .ruleId(rule.getRuleId())
                .transferType(request.getTransferType())
                .calculationDetails(calculationDetails)
                .build();
    }


    @Override
    public void recordCommission(UUID transactionId, UUID ruleId,
                                 Long amount, Currency currency, String calculationBasis) {
        log.info("Recording commission: transaction={}, amount={} {}", transactionId, amount, currency);

        CommissionTransaction commission = CommissionTransaction.builder()
                .transactionId(transactionId)
                .ruleId(ruleId)
                .amount(amount)
                .currency(currency)
                .calculationBasis(calculationBasis)
                .status(CommissionStatus.COMPLETED)
                .settled(false)
                .build();

        commissionTransactionRepository.save(commission);

        // Publish event
        eventPublisher.publishCommissionCollected(commission);

        log.info("Commission recorded successfully: {}", commission.getCommissionId());
    }

    @Override
    @Transactional(readOnly = true)
    public Long calculateBCEAOFee(Long amount) {
        return feeCalculationEngine.calculateBCEAOFee(amount);
    }

    @Override
    public void refundCommission(UUID transactionId) {
        log.info("Refunding commission for transaction: {}", transactionId);

        commissionTransactionRepository.findByTransactionId(transactionId)
                .ifPresent(commission -> {
                    commission.markAsRefunded();
                    commissionTransactionRepository.save(commission);

                    // Publish refund event
                    eventPublisher.publishCommissionRefunded(commission);

                    log.info("Commission refunded: {}", commission.getCommissionId());
                });
    }
}
