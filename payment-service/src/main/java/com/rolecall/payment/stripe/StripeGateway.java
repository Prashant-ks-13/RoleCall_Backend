package com.rolecall.payment.stripe;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;

import java.util.Map;

/**
 * Thin adapter over the Stripe SDK's static calls, so the service layer can
 * be unit-tested with a mock rather than needing to stub static methods.
 */
public interface StripeGateway {

    StripeCheckoutSession createCheckoutSession(long amountCents, String currency, String productName,
                                                 String successUrl, String cancelUrl,
                                                 Map<String, String> metadata) throws StripeException;

    Event verifyWebhookSignature(String payload, String sigHeader, String webhookSecret)
            throws SignatureVerificationException;
}
