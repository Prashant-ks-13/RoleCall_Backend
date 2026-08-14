package com.rolecall.application.event;

import com.rolecall.application.entity.ApplicationStatus;
import com.rolecall.application.entity.JobApplication;
import com.rolecall.common.event.ApplicationStatusChangedEvent;
import com.rolecall.common.event.ApplicationSubmittedEvent;
import com.rolecall.common.event.KafkaTopics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@Slf4j
public class ApplicationEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ApplicationEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishSubmitted(JobApplication application) {
        ApplicationSubmittedEvent event = new ApplicationSubmittedEvent(
                application.getId(), application.getJobId(), application.getCandidateId(), application.getCreatedAt());
        kafkaTemplate.send(KafkaTopics.APPLICATION_SUBMITTED, application.getJobId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish ApplicationSubmittedEvent for application {}", application.getId(), ex);
                    }
                });
    }

    public void publishStatusChanged(JobApplication application, ApplicationStatus fromStatus, UUID changedBy) {
        ApplicationStatusChangedEvent event = new ApplicationStatusChangedEvent(
                application.getId(), application.getJobId(), application.getCandidateId(),
                fromStatus == null ? null : fromStatus.name(), application.getStatus().name(), Instant.now());
        kafkaTemplate.send(KafkaTopics.APPLICATION_STATUS_CHANGED, application.getJobId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish ApplicationStatusChangedEvent for application {}", application.getId(), ex);
                    }
                });
    }
}
