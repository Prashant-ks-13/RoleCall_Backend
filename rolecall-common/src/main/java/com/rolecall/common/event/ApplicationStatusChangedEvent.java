package com.rolecall.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published by application-service on every status transition.
 * No consumer exists yet; this is a forward-compatible broadcast point
 * for a future notification/audit service.
 */
public record ApplicationStatusChangedEvent(
        UUID applicationId,
        UUID jobId,
        UUID candidateId,
        String fromStatus,
        String toStatus,
        Instant changedAt
) {
}
