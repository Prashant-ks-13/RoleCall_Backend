package com.rolecall.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published by application-service when a candidate applies to a job.
 * Consumed by job-service to increment the job's applicant count.
 */
public record ApplicationSubmittedEvent(
        UUID applicationId,
        UUID jobId,
        UUID candidateId,
        Instant submittedAt
) {
}
