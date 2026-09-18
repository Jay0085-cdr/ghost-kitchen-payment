package com.ghostkitchen.reconciliation;

import com.ghostkitchen.entity.ReconciliationResultStatus;
import com.ghostkitchen.security.AppUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class ReconciliationResultController {

    private final ReconciliationRunService reconciliationRunService;

    public ReconciliationResultController(ReconciliationRunService reconciliationRunService) {
        this.reconciliationRunService = reconciliationRunService;
    }

    @GetMapping("/api/reconciliation/results")
    public List<ReconciliationResultResponse> list(@RequestParam UUID runId,
                                                     @RequestParam(required = false) ReconciliationResultStatus status,
                                                     @AuthenticationPrincipal AppUserPrincipal principal) {
        return reconciliationRunService.getResults(runId, principal.getOrganizationId(), status);
    }

    @GetMapping("/api/reconciliation/results/{id}/discrepancies")
    public List<DiscrepancyResponse> discrepancies(@PathVariable UUID id, @AuthenticationPrincipal AppUserPrincipal principal) {
        return reconciliationRunService.getDiscrepancies(id, principal.getOrganizationId());
    }
}
