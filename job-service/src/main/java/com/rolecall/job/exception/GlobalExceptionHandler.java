package com.rolecall.job.exception;

import com.rolecall.common.error.ApiError;
import com.rolecall.common.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(JobNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(JobNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(NotJobOwnerException.class)
    public ResponseEntity<ApiError> handleForbidden(NotJobOwnerException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "Access denied", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiError.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiError.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        ApiError body = ApiError.ofFieldErrors(
                HttpStatus.BAD_REQUEST.value(), ErrorCode.VALIDATION_ERROR,
                "Validation failed", request.getRequestURI(), fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, "An unexpected error occurred", request);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, ErrorCode code, String message, HttpServletRequest request) {
        ApiError body = ApiError.of(status.value(), code, message, request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
