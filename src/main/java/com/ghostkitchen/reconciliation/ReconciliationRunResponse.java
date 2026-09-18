package com.ghostkitchen.reconciliation;

import com.ghostkitchen.entity.ReconciliationResultStatus;
import com.ghostkitchen.entity.ReconciliationRun;
import com.ghostkitchen.entity.ReconciliationRunStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record ReconciliationRunResponse(
        UUID id,
        UUID organizationId,
        String platformCode,
        String platformDisplayName,
        LocalDate periodStart,
        LocalDate periodEnd,
        Instant executedAt,
        ReconciliationRunStatus status,
        int totalResults,
        Map<ReconciliationResultStatus, Long> statusCounts
) {
    public static ReconciliationRunResponse from(ReconciliationRun run, int totalResults,
                                                   Map<ReconciliationResultStatus, Long> statusCounts) {
        return new ReconciliationRunResponse(
                run.getId(),
                run.getOrganization().getId(),
                run.getPlatform().getCode(),
                run.getPlatform().getDisplayName(),
                run.getPeriodStart(),
                run.getPeriodEnd(),
                run.getExecutedAt(),
                run.getStatus(),
                totalResults,
                statusCounts);
    }
}
