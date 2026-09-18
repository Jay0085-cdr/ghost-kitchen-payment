package com.ghostkitchen.reconciliation;

import com.ghostkitchen.entity.Discrepancy;
import com.ghostkitchen.entity.DiscrepancyCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DiscrepancyResponse(
        UUID id,
        UUID reconciliationResultId,
        DiscrepancyCategory category,
        BigDecimal expectedAmount,
        BigDecimal actualAmount,
        BigDecimal differenceAmount,
        String notes,
        Instant createdAt
) {
    public static DiscrepancyResponse from(Discrepancy discrepancy) {
        return new DiscrepancyResponse(
                discrepancy.getId(),
                discrepancy.getReconciliationResult().getId(),
                discrepancy.getCategory(),
                discrepancy.getExpectedAmount(),
                discrepancy.getActualAmount(),
                discrepancy.getDifferenceAmount(),
                discrepancy.getNotes(),
                discrepancy.getCreatedAt());
    }
}
