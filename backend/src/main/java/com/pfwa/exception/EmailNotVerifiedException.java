package com.pfwa.exception;

/**
 * Exception thrown when a user attempts to login without verifying their email.
 */
public class EmailNotVerifiedException extends RuntimeException {

    public EmailNotVerifiedException(String message) {
        super(message);
    }
}
