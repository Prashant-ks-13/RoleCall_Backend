package com.rolecall.payment.dto;

import com.rolecall.payment.entity.PaymentStatus;
import com.rolecall.payment.entity.PaymentTransaction;
import com.rolecall.payment.entity.PaymentType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentTransactionResponse(
        UUID id,
        UUID userId,
        UUID jobId,
        PaymentType type,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static PaymentTransactionResponse from(PaymentTransaction t) {
        return new PaymentTransactionResponse(
                t.getId(), t.getUserId(), t.getJobId(), t.getType(), t.getAmount(), t.getCurrency(),
                t.getStatus(), t.getCreatedAt(), t.getUpdatedAt());
    }
}
