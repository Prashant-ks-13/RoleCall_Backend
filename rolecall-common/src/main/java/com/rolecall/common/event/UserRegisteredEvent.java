package com.rolecall.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published by auth-service after a new user is persisted.
 * Consumed by user-service to create the profile shell row asynchronously.
 */
public record UserRegisteredEvent(
        UUID userId,
        String email,
        String role,
        Instant createdAt
) {
}
