package com.payment.commission.domain.entity;

import com.payment.commission.domain.enums.CommissionStatus;
import com.payment.common.enums.Currency;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Commission Transaction Entity
 * Tracks platform commission revenue per transaction.
 */
@Entity
@Table(name = "commission_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "commission_id")
    private UUID commissionId;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "rule_id")
    private UUID ruleId;

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 3)
    private Currency currency;

    @Column(name = "amount", nullable = false)
    private Long amount; // Commission amount in XOF/XAF

    @Type(JsonType.class)
    @Column(name = "calculation_basis", columnDefinition = "jsonb")
    private String calculationBasis;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private CommissionStatus status = CommissionStatus.COMPLETED;

    @Builder.Default
    @Column(name = "settled")
    private Boolean settled = false;

    @Column(name = "settlement_date")
    private LocalDateTime settlementDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Mark commission as settled
     */
    public void markAsSettled() {
        this.settled = true;
        this.settlementDate = LocalDateTime.now();
    }

    /**
     * Mark commission as refunded
     */
    public void markAsRefunded() {
        this.status = CommissionStatus.REFUNDED;
    }
}
