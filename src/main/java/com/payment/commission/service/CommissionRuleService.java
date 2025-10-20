package com.payment.commission.service;

import com.payment.commission.dto.request.CreateRuleRequest;
import com.payment.commission.dto.request.UpdateRuleRequest;
import com.payment.commission.dto.response.CommissionRuleResponse;
import com.payment.common.enums.Currency;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Commission Rule Service Interface
 * Handles management of commission rules
 */
public interface CommissionRuleService {

    /**
     * Create a new commission rule
     */
    CommissionRuleResponse createRule(CreateRuleRequest request, UUID createdBy);

    /**
     * Update an existing commission rule
     */
    CommissionRuleResponse updateRule(UUID ruleId, UpdateRuleRequest request);

    /**
     * Get a commission rule by ID
     */
    CommissionRuleResponse getRuleById(UUID ruleId);

    /**
     * Get all commission rules (paginated)
     */
    Page<CommissionRuleResponse> getAllRules(Pageable pageable);

    /**
     * Get commission rules by currency
     */
    List<CommissionRuleResponse> getRulesByCurrency(Currency currency);

    /**
     * Get active commission rules by currency
     */
    List<CommissionRuleResponse> getActiveRulesByCurrency(Currency currency);

    /**
     * Deactivate a commission rule
     */
    void deactivateRule(UUID ruleId);

    /**
     * Activate a commission rule
     */
    void activateRule(UUID ruleId);
}
