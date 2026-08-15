package com.rolecall.payment.razorpay;

import com.razorpay.RazorpayException;

import java.util.Map;

/**
 * Thin adapter over the Razorpay SDK, so the service layer can be
 * unit-tested with a mock rather than needing a live Razorpay account.
 */
public interface RazorpayGateway {

    /**
     * Creates a Razorpay Payment Link — a hosted checkout page URL, the
     * closest Razorpay equivalent to Stripe's Checkout Session. No
     * frontend SDK integration is required; the client is simply
     * redirected to {@code shortUrl}.
     */
    RazorpayPaymentLink createPaymentLink(long amountPaise, String currency, String description,
                                           String callbackUrl, Map<String, String> notes) throws RazorpayException;

    /**
     * Verifies the {@code X-Razorpay-Signature} header against the raw
     * webhook payload. Throws if the signature is missing/invalid.
     */
    void verifyWebhookSignature(String payload, String signature, String webhookSecret) throws RazorpayException;
}
