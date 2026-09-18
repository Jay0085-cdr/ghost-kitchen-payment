package com.ghostkitchen.ingestion.adapter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * What an adapter produces from a raw file — plain data, not the platform_transaction JPA
 * entity. ARCHITECTURE.md Section 6 sketches parse() returning List<PlatformTransaction>
 * directly, but that entity requires an already-persisted SettlementReport + Organization,
 * neither of which the adapter has (or should have — it has no business touching JPA/the
 * persistence context at all). SettlementReportService converts this into the entity once
 * the report row exists.
 */
public record ParsedTransaction(
        String platformOrderId,
        LocalDate orderDate,
        BigDecimal grossAmount,
        BigDecimal commission,
        BigDecimal advertisingFee,
        BigDecimal cancellationPenalty,
        BigDecimal taxAdjustment,
        BigDecimal otherDeduction,
        BigDecimal netExpectedPayout,
        String rawLineRef
) {
}
