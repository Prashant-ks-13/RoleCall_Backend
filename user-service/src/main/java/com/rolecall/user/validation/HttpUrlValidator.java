package com.rolecall.user.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.net.URI;
import java.net.URISyntaxException;

public class HttpUrlValidator implements ConstraintValidator<HttpUrl, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true; // pair with @NotBlank if presence is required
        }
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            boolean schemeOk = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
            boolean hostOk = uri.getHost() != null && !uri.getHost().isBlank();
            boolean noUserInfo = uri.getUserInfo() == null;
            return schemeOk && hostOk && noUserInfo;
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
