package com.pfwa.dto.transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * DTO containing percentage changes compared to previous period.
 */
public record PeriodChange(
        String income,
        String expenses,
        String net
) {
    /**
     * Calculates percentage change and formats it as a string.
     *
     * @param current  current period value
     * @param previous previous period value
     * @return formatted percentage change (e.g., "+11.1%", "-5.2%", "0.0%")
     */
    public static String calculateChange(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            if (current.compareTo(BigDecimal.ZERO) == 0) {
                return "0.0%";
            }
            return current.compareTo(BigDecimal.ZERO) > 0 ? "+100.0%" : "-100.0%";
        }

        BigDecimal change = current.subtract(previous)
                .divide(previous.abs(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP);

        String prefix = change.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
        return prefix + change + "%";
    }

    /**
     * Creates a PeriodChange by calculating changes between current and previous periods.
     *
     * @param currentIncome   current period income
     * @param previousIncome  previous period income
     * @param currentExpenses current period expenses
     * @param previousExpenses previous period expenses
     * @param currentNet      current period net
     * @param previousNet     previous period net
     * @return the period change DTO
     */
    public static PeriodChange calculate(
            BigDecimal currentIncome, BigDecimal previousIncome,
            BigDecimal currentExpenses, BigDecimal previousExpenses,
            BigDecimal currentNet, BigDecimal previousNet) {
        return new PeriodChange(
                calculateChange(currentIncome, previousIncome),
                calculateChange(currentExpenses, previousExpenses),
                calculateChange(currentNet, previousNet)
        );
    }
}
