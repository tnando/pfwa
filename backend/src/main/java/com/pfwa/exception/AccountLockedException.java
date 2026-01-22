package com.pfwa.exception;

import java.time.Instant;

/**
 * Exception thrown when attempting to access a locked account.
 */
public class AccountLockedException extends RuntimeException {

    private final Instant lockedUntil;

    public AccountLockedException(String message) {
        super(message);
        this.lockedUntil = null;
    }

    public AccountLockedException(String message, Instant lockedUntil) {
        super(message);
        this.lockedUntil = lockedUntil;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }
}
