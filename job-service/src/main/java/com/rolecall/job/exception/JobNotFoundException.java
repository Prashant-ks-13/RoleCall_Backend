package com.rolecall.job.exception;

import java.util.UUID;

public class JobNotFoundException extends RuntimeException {

    public JobNotFoundException(UUID id) {
        super("No job found with id '%s'".formatted(id));
    }
}
