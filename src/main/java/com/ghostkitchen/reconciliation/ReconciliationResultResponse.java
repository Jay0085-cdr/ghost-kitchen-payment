package com.ghostkitchen.reconciliation;

import com.ghostkitchen.entity.ReconciliationResult;
import com.ghostkitchen.entity.ReconciliationResultStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ReconciliationResultResponse(
        UUID id,
        UUID reconciliationRunId,
        UUID platformTransactionId,
        String platformOrderId,
        UUID bankTransactionId,
        BigDecimal expectedAmount,
        BigDecimal actualAmount,
        BigDecimal difference,
        ReconciliationResultStatus status,
        String explanation,
        Instant createdAt
) {
    public static ReconciliationResultResponse from(ReconciliationResult result) {
        return new ReconciliationResultResponse(
                result.getId(),
                result.getReconciliationRun().getId(),
                result.getPlatformTransaction() != null ? result.getPlatformTransaction().getId() : null,
                result.getPlatformTransaction() != null ? result.getPlatformTransaction().getPlatformOrderId() : null,
                result.getBankTransaction() != null ? result.getBankTransaction().getId() : null,
                result.getExpectedAmount(),
                result.getActualAmount(),
                result.getDifference(),
                result.getStatus(),
                result.getExplanation(),
                result.getCreatedAt());
    }
}
