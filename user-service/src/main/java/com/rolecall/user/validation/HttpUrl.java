package com.rolecall.user.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Requires the annotated string to be a well-formed http/https URL with a
 * non-empty host and no embedded userinfo (e.g. http://evil.com@trusted.com).
 * Rejects other schemes outright (javascript:, data:, file:, etc.) — this is
 * a server-side check independent of any validation the client performs, so
 * a non-browser client can't bypass it by calling the API directly.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = HttpUrlValidator.class)
public @interface HttpUrl {

    String message() default "must be a valid http or https URL";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
