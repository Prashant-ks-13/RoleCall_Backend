package com.rolecall.job.exception;

public class NotJobOwnerException extends RuntimeException {

    public NotJobOwnerException() {
        super("You do not own this job posting");
    }
}
