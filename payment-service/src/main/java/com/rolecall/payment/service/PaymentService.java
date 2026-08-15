package com.rolecall.payment.service;

import com.rolecall.payment.dto.CheckoutSessionRequest;
import com.rolecall.payment.dto.CheckoutSessionResponse;
import com.rolecall.payment.dto.PageResponse;
import com.rolecall.payment.dto.PaymentTransactionResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PaymentService {

    CheckoutSessionResponse createCheckoutSession(UUID userId, CheckoutSessionRequest request);

    /**
     * @param eventIdHeader the X-Razorpay-Event-Id header, or null if Razorpay didn't send one
     *                      for this webhook delivery (see PaymentServiceImpl for the fallback).
     */
    void handleWebhook(String payload, String signatureHeader, String eventIdHeader);

    PageResponse<PaymentTransactionResponse> getMine(UUID userId, Pageable pageable);

    PaymentTransactionResponse getById(UUID id, UUID requesterId, boolean requesterIsAdmin);
}
