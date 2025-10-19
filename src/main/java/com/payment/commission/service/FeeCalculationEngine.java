package com.payment.commission.service;

import com.payment.commission.domain.entity.CommissionRule;
import com.payment.common.enums.Currency;
import com.payment.common.enums.KYCLevel;
import com.payment.commission.domain.enums.TransferType;
import com.payment.commission.exception.NoMatchingRuleException;
import com.payment.commission.repository.CommissionRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Fee Calculation Engine
 * Core fee calculation logic implementing BCEAO rules
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FeeCalculationEngine {

    private final CommissionRuleRepository commissionRuleRepository;
    private final MessageService messageService;

    private static final Long BCEAO_FREE_THRESHOLD = 5000L; // XOF
    private static final Long BCEAO_FIXED_FEE = 100L;       // XOF
    private static final Double BCEAO_PERCENTAGE = 0.005;   // 0.5%
    private static final Long BCEAO_MAX_FEE = 1000L;        // XOF

    /**
     * Calculate fee using BCEAO rules
     * Rules:
     * - Amount <= 5,000 XOF: FREE (financial inclusion)
     * - Amount > 5,000 XOF: 100 XOF + 0.5%, capped at 1,000 XOF
     */
    public Long calculateBCEAOFee(Long amount) {
        // Financial inclusion: FREE for amounts <= 5,000 XOF
        if (amount <= BCEAO_FREE_THRESHOLD) {
            log.info("Amount {} XOF <= {} XOF: FREE (BCEAO financial inclusion)", amount, BCEAO_FREE_THRESHOLD);
            return 0L;
        }

        // Calculate: 100 XOF + 0.5% of amount
        Long percentageFee = Math.round(amount * BCEAO_PERCENTAGE);
        Long totalFee = BCEAO_FIXED_FEE + percentageFee;

        // Cap at 1,000 XOF
        Long finalFee = Math.min(totalFee, BCEAO_MAX_FEE);

        log.info("BCEAO fee for {} XOF: {} (fixed: {}, percentage: {}, capped at: {})",
                 amount, finalFee, BCEAO_FIXED_FEE, percentageFee, BCEAO_MAX_FEE);

        return finalFee;
    }

    /**
     * Calculate fee using custom commission rules
     */
    @Cacheable(value = "commission-calculation", key = "#amount + '-' + #providerId + '-' + #transferType")
    public Long calculateFee(Long amount, Currency currency, UUID providerId,
                            TransferType transferType, KYCLevel kycLevel) {
        log.info("Calculating fee: amount={}, currency={}, provider={}, type={}, kycLevel={}",
                 amount, currency, providerId, transferType, kycLevel);

        // Get active rules for this provider, currency, and transfer type, ordered by priority
        List<CommissionRule> rules = commissionRuleRepository.findActiveRulesByProviderAndCurrencyAndType(
            providerId, currency, transferType
        );

        if (rules.isEmpty()) {
            log.warn("No commission rules found for provider {} and transfer type {}, using BCEAO default",
                     providerId, transferType);
            return calculateBCEAOFee(amount);
        }

        // Find first matching rule
        CommissionRule matchingRule = rules.stream()
                .filter(rule -> rule.matches(amount, transferType, kycLevel))
                .findFirst()
                .orElseThrow(() -> new NoMatchingRuleException(
                    messageService.getMessage("error.no.matching.rule")
                ));

        Long fee = matchingRule.calculateFee(amount);
        log.info("Fee calculated using rule {}: {} XOF", matchingRule.getRuleId(), fee);

        return fee;
    }

    /**
     * Get matching rule for transaction
     */
    public CommissionRule findMatchingRule(Long amount, Currency currency, UUID providerId,
                                          TransferType transferType, KYCLevel kycLevel) {
        List<CommissionRule> rules = commissionRuleRepository.findActiveRulesByProviderAndCurrencyAndType(
            providerId, currency, transferType
        );

        return rules.stream()
                .filter(rule -> rule.matches(amount, transferType, kycLevel))
                .findFirst()
                .orElse(null);
    }
}
