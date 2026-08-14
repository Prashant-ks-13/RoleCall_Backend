package com.rolecall.payment.event;

import com.rolecall.common.event.KafkaTopics;
import com.rolecall.common.event.PaymentCompletedEvent;
import com.rolecall.common.event.PaymentFailedEvent;
import com.rolecall.payment.entity.PaymentTransaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Slf4j
public class PaymentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishCompleted(PaymentTransaction transaction) {
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                transaction.getId(), transaction.getUserId(), transaction.getJobId(),
                transaction.getType().name(), transaction.getAmount(), Instant.now());
        String key = transaction.getJobId() != null ? transaction.getJobId().toString() : transaction.getUserId().toString();
        kafkaTemplate.send(KafkaTopics.PAYMENT_COMPLETED, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish PaymentCompletedEvent for transaction {}", transaction.getId(), ex);
                    }
                });
    }

    public void publishFailed(PaymentTransaction transaction, String reason) {
        PaymentFailedEvent event = new PaymentFailedEvent(
                transaction.getId(), transaction.getUserId(), transaction.getJobId(), reason, Instant.now());
        String key = transaction.getJobId() != null ? transaction.getJobId().toString() : transaction.getUserId().toString();
        kafkaTemplate.send(KafkaTopics.PAYMENT_FAILED, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish PaymentFailedEvent for transaction {}", transaction.getId(), ex);
                    }
                });
    }
}
