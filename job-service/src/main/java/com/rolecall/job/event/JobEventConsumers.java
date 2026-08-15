package com.rolecall.job.event;

import com.rolecall.common.event.ApplicationSubmittedEvent;
import com.rolecall.common.event.KafkaTopics;
import com.rolecall.common.event.PaymentCompletedEvent;
import com.rolecall.common.event.PaymentFailedEvent;
import com.rolecall.job.service.JobService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * These consumers are wired now even though, at this point in the branch
 * sequence, no service produces {@code application.submitted} yet and only
 * a later branch (payment-service) produces the payment events. Kafka's
 * auto-topic-creation means subscribing here is harmless before a producer
 * exists — the consumer simply sits idle until messages arrive.
 */
@Component
@Slf4j
public class JobEventConsumers {

    private final JobService jobService;

    public JobEventConsumers(JobService jobService) {
        this.jobService = jobService;
    }

    @KafkaListener(topics = KafkaTopics.APPLICATION_SUBMITTED, groupId = "job-service")
    public void onApplicationSubmitted(ApplicationSubmittedEvent event) {
        log.info("Incrementing applicant count for job {} (application {})", event.jobId(), event.applicationId());
        jobService.incrementApplicantCount(event.jobId());
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_COMPLETED, groupId = "job-service")
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        if (event.jobId() == null) {
            return; // e.g. a SUBSCRIPTION payment unrelated to a specific job
        }
        log.info("Marking job {} as FEATURED following payment {}", event.jobId(), event.transactionId());
        jobService.markFeatured(event.jobId(), event.completedAt().plusSeconds(30L * 24 * 60 * 60));
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_FAILED, groupId = "job-service")
    public void onPaymentFailed(PaymentFailedEvent event) {
        if (event.jobId() == null) {
            return;
        }
        log.info("Reverting pending featured state for job {} after failed payment {}", event.jobId(), event.transactionId());
        jobService.revertFeatured(event.jobId());
    }
}
