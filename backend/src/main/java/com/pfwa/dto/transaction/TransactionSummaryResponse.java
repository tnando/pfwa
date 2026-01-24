package com.pfwa.dto.transaction;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Response DTO for transaction summary and statistics.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransactionSummaryResponse(
        SummaryPeriod period,
        SummaryTotals totals,
        CategoryBreakdown categoryBreakdown,
        PeriodComparison comparison
) {
    /**
     * Creates a TransactionSummaryResponse.
     *
     * @param period            the summary period
     * @param totals            the totals for the period
     * @param categoryBreakdown the category breakdown
     * @param comparison        the comparison with previous period (optional)
     * @return the transaction summary response
     */
    public static TransactionSummaryResponse of(
            SummaryPeriod period,
            SummaryTotals totals,
            CategoryBreakdown categoryBreakdown,
            PeriodComparison comparison) {
        return new TransactionSummaryResponse(period, totals, categoryBreakdown, comparison);
    }
}
