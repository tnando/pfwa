package com.pfwa.dto.transaction;

/**
 * DTO containing comparison data between current and previous periods.
 */
public record PeriodComparison(
        PreviousPeriodSummary previousPeriod,
        PeriodChange change
) {
    /**
     * Creates a PeriodComparison.
     *
     * @param previousPeriod the previous period summary
     * @param change         the percentage changes
     * @return the period comparison
     */
    public static PeriodComparison of(PreviousPeriodSummary previousPeriod, PeriodChange change) {
        return new PeriodComparison(previousPeriod, change);
    }
}
