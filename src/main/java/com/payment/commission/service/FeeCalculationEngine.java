package com.payment.commission.service;

import com.payment.commission.domain.entity.CommissionRule;
import com.payment.commission.exception.RuleNotFoundException;
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
import com.payment.common.i18n.MessageService;

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
     * Calculate fee using a specific commission rule by ID
     * @param ruleId The UUID of the commission rule to use
     * @param amount The transaction amount
     * @return The calculated fee amount
     */
    public Long calculateFeeByRuleId(UUID ruleId, Long amount) {
        log.info("Calculating fee using rule ID: {} for amount: {}", ruleId, amount);

        // Get the specific rule by ID
        CommissionRule rule = commissionRuleRepository.findById(ruleId)
                .orElseThrow(() -> new RuleNotFoundException(
                        messageService.getMessage("error.rule.not.found") + ": " + ruleId
                ));

        // Check if rule is active
        if (!rule.getIsActive()) {
            throw new RuleNotFoundException(
                    messageService.getMessage("error.rule.not.active") + ": " + ruleId
            );
        }

        // Check if rule is currently effective (within date range)
        if (!rule.isEffective()) {
            throw new RuleNotFoundException(
                    messageService.getMessage("error.rule.not.effective") + ": " + ruleId
            );
        }

        // Verify transaction amount is within min/max amount limits
        if (rule.getMinAmount() != null && amount < rule.getMinAmount()) {
            log.warn("Transaction amount {} is below minimum {} for rule {}", amount, rule.getMinAmount(), ruleId);
            throw new NoMatchingRuleException(
                    messageService.getMessage("error.amount.below.minimum") +
                    " (Min: " + rule.getMinAmount() + " " + rule.getCurrency() + ")"
            );
        }

        if (rule.getMaxAmount() != null && amount > rule.getMaxAmount()) {
            log.warn("Transaction amount {} exceeds maximum {} for rule {}", amount, rule.getMaxAmount(), ruleId);
            throw new NoMatchingRuleException(
                    messageService.getMessage("error.amount.above.maximum") +
                    " (Max: " + rule.getMaxAmount() + " " + rule.getCurrency() + ")"
            );
        }

        // Calculate fee using the rule
        Long fee = rule.calculateFee(amount);
        log.info("Fee calculated using rule {}: {} {}", rule.getRuleId(), fee, rule.getCurrency());

        return fee;
    }

    /**
     * Get commission rule by ID
     */
    public CommissionRule getRuleById(UUID ruleId) {
        return commissionRuleRepository.findById(ruleId)
                .orElseThrow(() -> new RuleNotFoundException(
                        messageService.getMessage("error.rule.not.found") + ": " + ruleId
                ));
    }

    /**
     * Get matching rule for transaction
     * @deprecated Use getRuleById with explicit ruleId instead
     */
    @Deprecated
    public CommissionRule findMatchingRule(Long amount, Currency currency,
                                           TransferType transferType, KYCLevel kycLevel) {
        List<CommissionRule> rules = commissionRuleRepository.findActiveRulesByCurrencyAndType(
                currency, transferType
        );

        return rules.stream()
                .filter(rule -> rule.matches(amount, transferType, kycLevel))
                .findFirst()
                .orElse(null);
    }
}
