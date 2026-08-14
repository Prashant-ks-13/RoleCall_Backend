package com.rolecall.auth.exception;

/**
 * Deliberately generic ("invalid email or password") for both a missing
 * account and a wrong password, to avoid leaking which one it was
 * (account enumeration).
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
