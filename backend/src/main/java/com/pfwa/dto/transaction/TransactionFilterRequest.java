package com.pfwa.dto.transaction;

import com.pfwa.entity.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for filtering transactions.
 * All filters are optional and combined with AND logic.
 */
public record TransactionFilterRequest(
        LocalDate startDate,
        LocalDate endDate,
        TransactionType type,
        List<UUID> categoryIds,
        @DecimalMin(value = "0", message = "Minimum amount cannot be negative")
        BigDecimal minAmount,
        @DecimalMin(value = "0", message = "Maximum amount cannot be negative")
        BigDecimal maxAmount,
        @Size(min = 2, max = 100, message = "Search term must be between 2 and 100 characters")
        String search
) {
    /**
     * Creates an empty filter (no filters applied).
     *
     * @return empty filter request
     */
    public static TransactionFilterRequest empty() {
        return new TransactionFilterRequest(null, null, null, null, null, null, null);
    }

    /**
     * Checks if any filter is applied.
     *
     * @return true if at least one filter is set
     */
    public boolean hasFilters() {
        return startDate != null || endDate != null || type != null ||
                (categoryIds != null && !categoryIds.isEmpty()) ||
                minAmount != null || maxAmount != null ||
                (search != null && !search.isBlank());
    }
}
