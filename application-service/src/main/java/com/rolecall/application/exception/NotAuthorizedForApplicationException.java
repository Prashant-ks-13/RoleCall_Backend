package com.rolecall.application.exception;

public class NotAuthorizedForApplicationException extends RuntimeException {

    public NotAuthorizedForApplicationException() {
        super("You are not authorized to view or modify this application");
    }
}
