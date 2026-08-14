package com.rolecall.common.error;

/**
 * Stable machine-readable error codes shared across services so clients
 * (the frontend, or other services) can branch on {@code error} without
 * parsing human-readable messages.
 */
public enum ErrorCode {
    VALIDATION_ERROR,
    BAD_REQUEST,
    UNAUTHORIZED,
    FORBIDDEN,
    NOT_FOUND,
    CONFLICT,
    INTERNAL_ERROR
}
