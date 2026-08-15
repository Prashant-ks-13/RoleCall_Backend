package com.rolecall.payment.exception;

import java.util.UUID;

public class PaymentTransactionNotFoundException extends RuntimeException {

    public PaymentTransactionNotFoundException(UUID id) {
        super("No payment transaction found with id '%s'".formatted(id));
    }
}
