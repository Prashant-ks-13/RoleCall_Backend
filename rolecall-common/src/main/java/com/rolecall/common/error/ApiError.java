package com.rolecall.common.error;

import java.time.Instant;
import java.util.List;

/**
 * Standard error response body returned by every service's
 * {@code @RestControllerAdvice}, so clients get one consistent JSON shape.
 */
public record ApiError(
        Instant timestamp,
        int status,
        ErrorCode error,
        String message,
        String path,
        List<FieldError> fieldErrors
) {
    public record FieldError(String field, String message) {
    }

    public static ApiError of(int status, ErrorCode error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path, List.of());
    }

    public static ApiError ofFieldErrors(int status, ErrorCode error, String message, String path, List<FieldError> fieldErrors) {
        return new ApiError(Instant.now(), status, error, message, path, fieldErrors);
    }
}
