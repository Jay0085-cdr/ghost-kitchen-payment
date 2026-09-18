package com.ghostkitchen.reconciliation;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record TriggerReconciliationRequest(
        @NotNull(message = "platformId is required") UUID platformId,
        @NotNull(message = "periodStart is required") LocalDate periodStart,
        @NotNull(message = "periodEnd is required") LocalDate periodEnd
) {
}
