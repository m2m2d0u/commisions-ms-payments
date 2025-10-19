package com.payment.commission.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when a commission is settled
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionSettledEvent {

    private UUID commissionId;
    private UUID transactionId;
    private UUID providerId;
    private Long amount;
    private String currency;
    private LocalDateTime settlementDate;
    private LocalDateTime timestamp;

    public static CommissionSettledEvent create(UUID commissionId, UUID transactionId,
                                                 UUID providerId, Long amount, String currency,
                                                 LocalDateTime settlementDate) {
        return CommissionSettledEvent.builder()
                .commissionId(commissionId)
                .transactionId(transactionId)
                .providerId(providerId)
                .amount(amount)
                .currency(currency)
                .settlementDate(settlementDate)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
