package com.ghostkitchen.reconciliation;

import java.math.BigDecimal;

/**
 * Deterministic matching parameters (ARCHITECTURE.md Section 7 step 5). Settlements commonly
 * lag the order date and small rounding differences are expected, so both the date window and
 * the amount tolerance are configurable rather than requiring an exact match.
 */
public record ReconciliationConfig(int dateWindowDays, BigDecimal amountTolerance) {

    public static ReconciliationConfig defaults() {
        return new ReconciliationConfig(7, new BigDecimal("1.00"));
    }
}
