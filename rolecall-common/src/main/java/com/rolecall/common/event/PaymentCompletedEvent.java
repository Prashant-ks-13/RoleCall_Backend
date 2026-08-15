package com.rolecall.common.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Published by payment-service once a Stripe webhook confirms a successful payment.
 * Consumed by job-service to activate a featured listing.
 */
public record PaymentCompletedEvent(
        UUID transactionId,
        UUID userId,
        UUID jobId,
        String type,
        BigDecimal amount,
        Instant completedAt
) {
}
