package com.payment.commission.domain.entity;

import com.payment.common.enums.Currency;
import com.payment.common.enums.KYCLevel;
import com.payment.commission.domain.enums.TransferType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Commission Rule Entity
 * Stores commission calculation rules as a wallet containing commission rules.
 */
@Entity
@Table(name = "commission_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "rule_id")
    private UUID ruleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 3)
    private Currency currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "transfer_type", nullable = false, length = 20)
    private TransferType transferType;

    @Column(name = "min_transaction")
    private Long minTransaction;

    @Column(name = "max_transaction")
    private Long maxTransaction;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_level", length = 20)
    private KYCLevel kycLevel;

    // Commission calculation parameters
    @Column(name = "percentage", nullable = false, precision = 5, scale = 4)
    private BigDecimal percentage; // e.g., 0.0050 for 0.5%

    @Builder.Default
    @Column(name = "fixed_amount")
    private Long fixedAmount = 0L; // e.g., 100 XOF

    @Builder.Default
    @Column(name = "min_amount")
    private Long minAmount = 0L;

    @Column(name = "max_amount")
    private Long maxAmount;

    // Rule management
    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "priority")
    private Integer priority = 0;

    @Column(name = "effective_from", nullable = false)
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    // Metadata
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (effectiveFrom == null) {
            effectiveFrom = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Check if the rule is currently effective
     */
    public boolean isEffective() {
        LocalDateTime now = LocalDateTime.now();
        return isActive &&
               !now.isBefore(effectiveFrom) &&
               (effectiveTo == null || now.isBefore(effectiveTo));
    }

    /**
     * Check if this rule matches the transaction criteria
     */
    public boolean matches(Long amount, TransferType type, KYCLevel userKycLevel) {
        if (!isEffective() || transferType != type) {
            return false;
        }

        // Check amount range
        if (minTransaction != null && amount < minTransaction) {
            return false;
        }
        if (maxTransaction != null && amount > maxTransaction) {
            return false;
        }

        // Check KYC level
        if (kycLevel != null && kycLevel != KYCLevel.ANY && kycLevel != userKycLevel) {
            return false;
        }

        return true;
    }

    /**
     * Calculate fee for the given amount using this rule
     */
    public Long calculateFee(Long amount) {
        // Calculate percentage fee
        BigDecimal percentageFee = BigDecimal.valueOf(amount)
                .multiply(percentage)
                .setScale(0, java.math.RoundingMode.DOWN);

        // Add fixed amount
        long totalFee = percentageFee.longValue() + (fixedAmount != null ? fixedAmount : 0);

        // Apply minimum
        if (minAmount != null && totalFee < minAmount) {
            totalFee = minAmount;
        }

        // Apply maximum
        if (maxAmount != null && totalFee > maxAmount) {
            totalFee = maxAmount;
        }

        return totalFee;
    }
}
