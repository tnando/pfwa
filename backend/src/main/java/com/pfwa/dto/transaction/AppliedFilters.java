package com.pfwa.dto.transaction;

import com.pfwa.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO representing the filters that were applied to a transaction query.
 */
public record AppliedFilters(
        LocalDate startDate,
        LocalDate endDate,
        TransactionType type,
        List<UUID> categoryIds,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        String search
) {
    /**
     * Creates an AppliedFilters from a TransactionFilterRequest.
     *
     * @param filter the filter request
     * @return the applied filters DTO
     */
    public static AppliedFilters from(TransactionFilterRequest filter) {
        return new AppliedFilters(
                filter.startDate(),
                filter.endDate(),
                filter.type(),
                filter.categoryIds() != null ? filter.categoryIds() : List.of(),
                filter.minAmount(),
                filter.maxAmount(),
                filter.search()
        );
    }
}
