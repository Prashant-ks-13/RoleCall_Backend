package com.rolecall.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published by payment-service when a Stripe payment fails.
 * Consumed by job-service to revert any pending "featuring" state.
 */
public record PaymentFailedEvent(
        UUID transactionId,
        UUID userId,
        UUID jobId,
        String reason,
        Instant failedAt
) {
}
