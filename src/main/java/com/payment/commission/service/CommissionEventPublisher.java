package com.payment.commission.service;

import com.payment.commission.domain.entity.CommissionTransaction;
import com.payment.kafka.event.CommissionEvent;
import com.payment.kafka.config.KafkaTopics;
import com.payment.kafka.publisher.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service for publishing commission-related events to Kafka
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommissionEventPublisher {

    private final EventPublisher eventPublisher;


    /**
     * Publish commission collected event
     */
    public void publishCommissionCollected(CommissionTransaction commission) {
        try {
            CommissionEvent event = CommissionEvent.commissionCollected(
                    commission.getCommissionId().toString(),
                    commission.getTransactionId().toString(),
                    commission.getAmount(),
                    commission.getCurrency().name(),
                    commission.getCalculationBasis(),
                    LocalDateTime.now()
            );

            eventPublisher.publish(KafkaTopics.COMMISSION_COLLECTED, commission.getCommissionId().toString(), event);
            log.info("Published COMMISSION_COLLECTED event for commission: {}", commission.getCommissionId());
        } catch (Exception e) {
            log.error("Error publishing COMMISSION_COLLECTED event", e);
        }
    }

    /**
     * Publish commission refunded event
     */
    public void publishCommissionRefunded(CommissionTransaction commission) {
        try {
            CommissionEvent event = CommissionEvent.commissionRefunded(
                    commission.getCommissionId().toString(),
                    commission.getTransactionId().toString(),
                    commission.getAmount(),
                    commission.getCurrency().name(),
                    LocalDateTime.now()
            );

            eventPublisher.publish(KafkaTopics.COMMISSION_REFUNDED, commission.getCommissionId().toString(), event);
            log.info("Published COMMISSION_REFUNDED event for commission: {}", commission.getCommissionId());
        } catch (Exception e) {
            log.error("Error publishing COMMISSION_REFUNDED event", e);
        }
    }

    /**
     * Publish commission settled event
     */
    public void publishCommissionSettled(CommissionTransaction commission) {
        try {
            CommissionEvent event = CommissionEvent.commissionSettled(
                    commission.getCommissionId().toString(),
                    commission.getTransactionId().toString(),
                    commission.getAmount(),
                    commission.getCurrency().name(),
                    commission.getSettlementDate(),
                    LocalDateTime.now()
            );

            eventPublisher.publish(KafkaTopics.COMMISSION_SETTLED, commission.getCommissionId().toString(), event);
            log.info("Published COMMISSION_SETTLED event for commission: {}", commission.getCommissionId());
        } catch (Exception e) {
            log.error("Error publishing COMMISSION_SETTLED event", e);
        }
    }
}
