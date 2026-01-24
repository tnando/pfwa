package com.pfwa.exception;

import java.util.UUID;

/**
 * Exception thrown when a transaction is not found or user does not have access.
 */
public class TransactionNotFoundException extends RuntimeException {

    private final UUID transactionId;

    public TransactionNotFoundException(UUID transactionId) {
        super("Transaction not found");
        this.transactionId = transactionId;
    }

    public TransactionNotFoundException(String message) {
        super(message);
        this.transactionId = null;
    }

    public UUID getTransactionId() {
        return transactionId;
    }
}
