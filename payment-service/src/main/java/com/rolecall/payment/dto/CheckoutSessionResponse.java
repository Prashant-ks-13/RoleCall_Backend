package com.rolecall.payment.dto;

import java.util.UUID;

public record CheckoutSessionResponse(
        UUID transactionId,
        String checkoutUrl
) {
}
