package com.pfwa.dto.transaction;

import java.math.BigDecimal;

/**
 * DTO for a single category's breakdown in the summary.
 */
public record CategoryBreakdownItem(
        CategorySummary category,
        BigDecimal total,
        BigDecimal percentage,
        int transactionCount
) {
    /**
     * Creates a CategoryBreakdownItem.
     *
     * @param category         the category summary
     * @param total            the total amount for this category
     * @param percentage       the percentage of total for this type
     * @param transactionCount the number of transactions in this category
     * @return the category breakdown item
     */
    public static CategoryBreakdownItem of(
            CategorySummary category,
            BigDecimal total,
            BigDecimal percentage,
            int transactionCount) {
        return new CategoryBreakdownItem(category, total, percentage, transactionCount);
    }
}
