package com.payment.commission.service;

import com.payment.commission.domain.entity.CommissionTransaction;
import com.payment.commission.event.CommissionCollectedEvent;
import com.payment.commission.event.CommissionRefundedEvent;
import com.payment.commission.event.CommissionSettledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for publishing commission-related events to Kafka
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommissionEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String COMMISSION_EVENTS_TOPIC = "commission-events";

    /**
     * Publish commission collected event
     */
    public void publishCommissionCollected(CommissionTransaction commission) {
        try {
            CommissionCollectedEvent event = CommissionCollectedEvent.create(
                    commission.getCommissionId(),
                    commission.getTransactionId(),
                    commission.getProviderId(),
                    commission.getAmount(),
                    commission.getCurrency().name(),
                    commission.getCalculationBasis()
            );

            kafkaTemplate.send(COMMISSION_EVENTS_TOPIC, event);
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
            CommissionRefundedEvent event = CommissionRefundedEvent.create(
                    commission.getCommissionId(),
                    commission.getTransactionId(),
                    commission.getProviderId(),
                    commission.getAmount(),
                    commission.getCurrency().name()
            );

            kafkaTemplate.send(COMMISSION_EVENTS_TOPIC, event);
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
            CommissionSettledEvent event = CommissionSettledEvent.create(
                    commission.getCommissionId(),
                    commission.getTransactionId(),
                    commission.getProviderId(),
                    commission.getAmount(),
                    commission.getCurrency().name(),
                    commission.getSettlementDate()
            );

            kafkaTemplate.send(COMMISSION_EVENTS_TOPIC, event);
            log.info("Published COMMISSION_SETTLED event for commission: {}", commission.getCommissionId());
        } catch (Exception e) {
            log.error("Error publishing COMMISSION_SETTLED event", e);
        }
    }
}
