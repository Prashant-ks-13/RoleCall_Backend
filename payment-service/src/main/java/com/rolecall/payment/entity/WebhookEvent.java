package com.rolecall.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Idempotency ledger: Razorpay may redeliver the same webhook event, and
 * this table's unique constraint on razorpayEventId ensures it's only ever
 * processed once. Sourced from the X-Razorpay-Event-Id header when present;
 * see PaymentServiceImpl for the fallback when it's absent.
 */
@Entity
@Table(name = "webhook_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookEvent {

    @Id
    private UUID id;

    @Column(name = "razorpay_event_id", nullable = false, unique = true, length = 255)
    private String razorpayEventId;

    @Column(nullable = false, length = 100)
    private String type;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (receivedAt == null) {
            receivedAt = Instant.now();
        }
    }
}
