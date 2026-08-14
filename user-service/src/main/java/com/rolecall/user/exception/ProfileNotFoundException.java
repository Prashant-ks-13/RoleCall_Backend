package com.rolecall.user.exception;

import java.util.UUID;

public class ProfileNotFoundException extends RuntimeException {

    public ProfileNotFoundException(UUID id) {
        super("No profile found for user id '%s'".formatted(id));
    }
}
