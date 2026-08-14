package com.rolecall.payment.exception;

public class NotAuthorizedForPaymentException extends RuntimeException {

    public NotAuthorizedForPaymentException(String message) {
        super(message);
    }
}
