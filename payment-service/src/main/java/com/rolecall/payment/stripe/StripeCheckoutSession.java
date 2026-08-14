package com.rolecall.payment.stripe;

public record StripeCheckoutSession(String sessionId, String checkoutUrl) {
}
