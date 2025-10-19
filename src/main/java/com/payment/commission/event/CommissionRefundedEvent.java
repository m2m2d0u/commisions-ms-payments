package com.payment.commission.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when a commission is refunded
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionRefundedEvent {

    private UUID commissionId;
    private UUID transactionId;
    private UUID providerId;
    private Long amount;
    private String currency;
    private LocalDateTime timestamp;

    public static CommissionRefundedEvent create(UUID commissionId, UUID transactionId,
                                                  UUID providerId, Long amount, String currency) {
        return CommissionRefundedEvent.builder()
                .commissionId(commissionId)
                .transactionId(transactionId)
                .providerId(providerId)
                .amount(amount)
                .currency(currency)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
