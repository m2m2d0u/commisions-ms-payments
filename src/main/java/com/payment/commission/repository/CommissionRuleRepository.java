package com.payment.commission.repository;

import com.payment.commission.domain.entity.CommissionRule;
import com.payment.common.enums.Currency;
import com.payment.commission.domain.enums.TransferType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Commission Rules
 */
@Repository
public interface CommissionRuleRepository extends JpaRepository<CommissionRule, UUID> {

    /**
     * Find all active rules for a provider and currency, ordered by priority (descending)
     */
    @Query("SELECT cr FROM CommissionRule cr " +
           "WHERE cr.providerId = :providerId " +
           "AND cr.currency = :currency " +
           "AND cr.isActive = true " +
           "AND CURRENT_TIMESTAMP >= cr.effectiveFrom " +
           "AND (cr.effectiveTo IS NULL OR CURRENT_TIMESTAMP < cr.effectiveTo) " +
           "ORDER BY cr.priority DESC")
    List<CommissionRule> findActiveRulesByProviderAndCurrency(
            @Param("providerId") UUID providerId,
            @Param("currency") Currency currency
    );

    /**
     * Find active rules by provider, currency, and transfer type, ordered by priority
     */
    @Query("SELECT cr FROM CommissionRule cr " +
           "WHERE cr.providerId = :providerId " +
           "AND cr.currency = :currency " +
           "AND cr.transferType = :transferType " +
           "AND cr.isActive = true " +
           "AND CURRENT_TIMESTAMP >= cr.effectiveFrom " +
           "AND (cr.effectiveTo IS NULL OR CURRENT_TIMESTAMP < cr.effectiveTo) " +
           "ORDER BY cr.priority DESC")
    List<CommissionRule> findActiveRulesByProviderAndCurrencyAndType(
            @Param("providerId") UUID providerId,
            @Param("currency") Currency currency,
            @Param("transferType") TransferType transferType
    );

    /**
     * Find all rules for a provider (active and inactive)
     */
    List<CommissionRule> findByProviderId(UUID providerId);

    /**
     * Find rules by provider and active status
     */
    List<CommissionRule> findByProviderIdAndIsActive(UUID providerId, Boolean isActive);

    /**
     * Find rules by currency
     */
    List<CommissionRule> findByCurrency(Currency currency);

    /**
     * Find rules by transfer type
     */
    List<CommissionRule> findByTransferType(TransferType transferType);
}
