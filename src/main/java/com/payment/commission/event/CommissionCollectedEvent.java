package com.payment.commission.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when a commission is collected
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionCollectedEvent {

    private UUID commissionId;
    private UUID transactionId;
    private UUID providerId;
    private Long amount;
    private String currency;
    private String calculationBasis;
    private LocalDateTime timestamp;

    public static CommissionCollectedEvent create(UUID commissionId, UUID transactionId,
                                                  UUID providerId, Long amount, String currency,
                                                  String calculationBasis) {
        return CommissionCollectedEvent.builder()
                .commissionId(commissionId)
                .transactionId(transactionId)
                .providerId(providerId)
                .amount(amount)
                .currency(currency)
                .calculationBasis(calculationBasis)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
