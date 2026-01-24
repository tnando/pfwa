package com.pfwa.dto.transaction;

import com.pfwa.entity.Transaction;
import com.pfwa.entity.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Response DTO for a single transaction.
 */
public record TransactionResponse(
        UUID id,
        BigDecimal amount,
        TransactionType type,
        CategoryResponse category,
        LocalDate date,
        String description,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    /**
     * Creates a TransactionResponse from a Transaction entity.
     *
     * @param transaction the transaction entity
     * @return the transaction response DTO
     */
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAmount(),
                transaction.getType(),
                CategoryResponse.from(transaction.getCategory()),
                transaction.getTransactionDate(),
                transaction.getDescription(),
                transaction.getNotes(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt()
        );
    }
}
