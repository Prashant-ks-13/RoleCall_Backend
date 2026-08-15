package com.rolecall.payment.repository;

import com.rolecall.payment.entity.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {

    boolean existsByRazorpayEventId(String razorpayEventId);
}
