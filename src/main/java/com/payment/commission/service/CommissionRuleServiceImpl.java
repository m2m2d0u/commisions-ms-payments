package com.payment.commission.service;

import com.payment.commission.domain.entity.CommissionRule;
import com.payment.commission.dto.request.CreateRuleRequest;
import com.payment.commission.dto.request.UpdateRuleRequest;
import com.payment.commission.dto.response.CommissionRuleResponse;
import com.payment.commission.exception.InvalidRuleException;
import com.payment.commission.exception.RuleNotFoundException;
import com.payment.commission.mapper.CommissionRuleMapper;
import com.payment.commission.repository.CommissionRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.payment.common.i18n.MessageService;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Commission Rule Service Implementation
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CommissionRuleServiceImpl implements CommissionRuleService {

    private final CommissionRuleRepository commissionRuleRepository;
    private final CommissionRuleMapper commissionRuleMapper;
    private final MessageService messageService;

    @Override
    @CacheEvict(value = "commission-calculation", allEntries = true)
    public CommissionRuleResponse createRule(CreateRuleRequest request, UUID createdBy) {
        log.info("Creating commission rule");

        // Validate rule
        validateRule(request);

        CommissionRule rule = CommissionRule.builder()
                .currency(request.getCurrency())
                .transferType(request.getTransferType())
                .minTransaction(request.getMinTransaction())
                .maxTransaction(request.getMaxTransaction())
                .kycLevel(request.getKycLevel())
                .percentage(request.getPercentage())
                .fixedAmount(request.getFixedAmount() != null ? request.getFixedAmount() : 0L)
                .minAmount(request.getMinAmount() != null ? request.getMinAmount() : 0L)
                .maxAmount(request.getMaxAmount())
                .isActive(true)
                .priority(request.getPriority() != null ? request.getPriority() : 0)
                .effectiveFrom(request.getEffectiveFrom() != null ? request.getEffectiveFrom() : LocalDateTime.now())
                .effectiveTo(request.getEffectiveTo())
                .description(request.getDescription())
                .notes(request.getNotes())
                .createdBy(createdBy)
                .build();

        CommissionRule savedRule = commissionRuleRepository.save(rule);
        log.info("Commission rule created successfully: {}", savedRule.getRuleId());

        return commissionRuleMapper.toResponse(savedRule);
    }

    @Override
    @CacheEvict(value = "commission-calculation", allEntries = true)
    public CommissionRuleResponse updateRule(UUID ruleId, UpdateRuleRequest request) {
        log.info("Updating commission rule: {}", ruleId);

        CommissionRule rule = commissionRuleRepository.findById(ruleId)
                .orElseThrow(() -> new RuleNotFoundException(
                    messageService.getMessage("error.rule.not.found") + ": " + ruleId
                ));

        // Update fields if provided
        if (request.getPercentage() != null) {
            rule.setPercentage(request.getPercentage());
        }
        if (request.getFixedAmount() != null) {
            rule.setFixedAmount(request.getFixedAmount());
        }
        if (request.getMinAmount() != null) {
            rule.setMinAmount(request.getMinAmount());
        }
        if (request.getMaxAmount() != null) {
            rule.setMaxAmount(request.getMaxAmount());
        }
        if (request.getIsActive() != null) {
            rule.setIsActive(request.getIsActive());
        }
        if (request.getPriority() != null) {
            rule.setPriority(request.getPriority());
        }
        if (request.getEffectiveTo() != null) {
            rule.setEffectiveTo(request.getEffectiveTo());
        }
        if (request.getDescription() != null) {
            rule.setDescription(request.getDescription());
        }
        if (request.getNotes() != null) {
            rule.setNotes(request.getNotes());
        }

        CommissionRule updatedRule = commissionRuleRepository.save(rule);
        log.info("Commission rule updated successfully: {}", ruleId);

        return commissionRuleMapper.toResponse(updatedRule);
    }

    @Override
    @Transactional(readOnly = true)
    public CommissionRuleResponse getRuleById(UUID ruleId) {
        CommissionRule rule = commissionRuleRepository.findById(ruleId)
                .orElseThrow(() -> new RuleNotFoundException(
                    messageService.getMessage("error.rule.not.found") + ": " + ruleId
                ));
        return commissionRuleMapper.toResponse(rule);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommissionRuleResponse> getAllRules(Pageable pageable) {
        return commissionRuleRepository.findAll(pageable)
                .map(commissionRuleMapper::toResponse);
    }


    @Override
    @CacheEvict(value = "commission-calculation", allEntries = true)
    public void deactivateRule(UUID ruleId) {
        log.info("Deactivating commission rule: {}", ruleId);

        CommissionRule rule = commissionRuleRepository.findById(ruleId)
                .orElseThrow(() -> new RuleNotFoundException(
                    messageService.getMessage("error.rule.not.found") + ": " + ruleId
                ));

        rule.setIsActive(false);
        commissionRuleRepository.save(rule);

        log.info("Commission rule deactivated: {}", ruleId);
    }

    @Override
    @CacheEvict(value = "commission-calculation", allEntries = true)
    public void activateRule(UUID ruleId) {
        log.info("Activating commission rule: {}", ruleId);

        CommissionRule rule = commissionRuleRepository.findById(ruleId)
                .orElseThrow(() -> new RuleNotFoundException(
                    messageService.getMessage("error.rule.not.found") + ": " + ruleId
                ));

        rule.setIsActive(true);
        commissionRuleRepository.save(rule);

        log.info("Commission rule activated: {}", ruleId);
    }

    private void validateRule(CreateRuleRequest request) {
        // Validate that minAmount <= maxAmount
        if (request.getMaxAmount() != null && request.getMinAmount() != null) {
            if (request.getMaxAmount() < request.getMinAmount()) {
                throw new InvalidRuleException(messageService.getMessage("error.rule.max.less.than.min"));
            }
        }

        // Validate that minTransaction <= maxTransaction
        if (request.getMaxTransaction() != null && request.getMinTransaction() != null) {
            if (request.getMaxTransaction() < request.getMinTransaction()) {
                throw new InvalidRuleException(messageService.getMessage("error.rule.max.transaction.less.than.min"));
            }
        }

        // Validate that effectiveFrom <= effectiveTo
        if (request.getEffectiveTo() != null && request.getEffectiveFrom() != null) {
            if (request.getEffectiveTo().isBefore(request.getEffectiveFrom())) {
                throw new InvalidRuleException(messageService.getMessage("error.rule.effective.to.before.from"));
            }
        }
    }
}
