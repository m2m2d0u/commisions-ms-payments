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
        log.info("Calculating fee for request: {}", request);

        Long feeAmount = feeCalculationEngine.calculateFee(
                request.getAmount(),
                request.getCurrency(),
                request.getTransferType(),
                request.getKycLevel()
        );

        CommissionRule matchingRule = feeCalculationEngine.findMatchingRule(
                request.getAmount(),
                request.getCurrency(),
                request.getTransferType(),
                request.getKycLevel()
        );

        // Build calculation details
        Map<String, Object> calculationDetails = new HashMap<>();
        if (matchingRule != null) {
            calculationDetails.put("ruleId", matchingRule.getRuleId());
            calculationDetails.put("transferType", request.getTransferType());
            calculationDetails.put("percentageFee", matchingRule.getPercentage());
            calculationDetails.put("fixedAmount", matchingRule.getFixedAmount());
            calculationDetails.put("minAmount", matchingRule.getMinAmount());
            calculationDetails.put("maxAmount", matchingRule.getMaxAmount());
            calculationDetails.put("finalAmount", feeAmount);
        } else {
            // BCEAO default rule applied
            calculationDetails.put("reason", "BCEAO_DEFAULT");
            if (request.getAmount() <= 5000L) {
                calculationDetails.put("message", messageService.getMessage("message.bceao.free.transaction"));
            }
        }

        return FeeCalculationResponse.builder()
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .commissionAmount(feeAmount)
                .ruleId(matchingRule != null ? matchingRule.getRuleId() : null)
                .transferType(request.getTransferType())
                .calculationDetails(calculationDetails)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Long calculateFee(Long amount, Currency currency,
                             TransferType transferType, KYCLevel kycLevel) {
        return feeCalculationEngine.calculateFee(amount, currency, transferType, kycLevel);
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
