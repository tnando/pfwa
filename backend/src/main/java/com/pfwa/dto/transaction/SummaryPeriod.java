package com.pfwa.dto.transaction;

import java.time.LocalDate;

/**
 * DTO representing a time period for summary statistics.
 */
public record SummaryPeriod(
        LocalDate startDate,
        LocalDate endDate
) {
    /**
     * Creates a SummaryPeriod with the given dates.
     *
     * @param startDate the start date of the period
     * @param endDate   the end date of the period
     * @return the summary period
     */
    public static SummaryPeriod of(LocalDate startDate, LocalDate endDate) {
        return new SummaryPeriod(startDate, endDate);
    }
}
