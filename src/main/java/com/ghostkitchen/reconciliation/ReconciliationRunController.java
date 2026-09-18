package com.ghostkitchen.reconciliation;

import com.ghostkitchen.security.AppUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reconciliation/runs")
public class ReconciliationRunController {

    private final ReconciliationRunService reconciliationRunService;

    public ReconciliationRunController(ReconciliationRunService reconciliationRunService) {
        this.reconciliationRunService = reconciliationRunService;
    }

    @PostMapping
    public ResponseEntity<ReconciliationRunResponse> trigger(@Valid @RequestBody TriggerReconciliationRequest request,
                                                               @AuthenticationPrincipal AppUserPrincipal principal) {
        ReconciliationRunResponse response = reconciliationRunService.triggerRun(principal.getOrganizationId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ReconciliationRunResponse get(@PathVariable UUID id, @AuthenticationPrincipal AppUserPrincipal principal) {
        return reconciliationRunService.getOwnedRun(id, principal.getOrganizationId());
    }

    @GetMapping
    public List<ReconciliationRunResponse> list(@AuthenticationPrincipal AppUserPrincipal principal) {
        return reconciliationRunService.listMine(principal.getOrganizationId());
    }
}
