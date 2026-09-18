package com.ghostkitchen.reconciliation;

import com.ghostkitchen.entity.Discrepancy;
import com.ghostkitchen.entity.ReconciliationResult;

import java.util.List;

/**
 * One platform_transaction's or bank_transaction's verdict, plus the discrepancy breakdown
 * behind it (empty for MATCHED). Both are unpersisted entities — a future Phase 6 service
 * saves them; this package never touches a repository.
 */
public record ReconciliationOutcome(ReconciliationResult result, List<Discrepancy> discrepancies) {
}
