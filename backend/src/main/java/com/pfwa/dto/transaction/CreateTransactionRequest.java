package com.pfwa.dto.transaction;

import com.pfwa.entity.TransactionType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for creating a new transaction.
 */
public record CreateTransactionRequest(
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        @DecimalMax(value = "10000000", message = "Amount exceeds maximum allowed value")
        BigDecimal amount,

        @NotNull(message = "Transaction type is required")
        TransactionType type,

        @NotNull(message = "Category is required")
        UUID categoryId,

        @NotNull(message = "Date is required")
        LocalDate date,

        @Size(max = 500, message = "Description must be 500 characters or less")
        String description,

        @Size(max = 1000, message = "Notes must be 1000 characters or less")
        String notes
) {
}
