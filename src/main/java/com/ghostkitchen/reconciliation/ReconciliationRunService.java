package com.ghostkitchen.reconciliation;

import com.ghostkitchen.entity.BankTransaction;
import com.ghostkitchen.entity.Organization;
import com.ghostkitchen.entity.Platform;
import com.ghostkitchen.entity.PlatformTransaction;
import com.ghostkitchen.entity.ReconciliationResult;
import com.ghostkitchen.entity.ReconciliationResultStatus;
import com.ghostkitchen.entity.ReconciliationRun;
import com.ghostkitchen.entity.ReconciliationRunStatus;
import com.ghostkitchen.exception.InvalidRequestException;
import com.ghostkitchen.exception.ResourceNotFoundException;
import com.ghostkitchen.repository.BankTransactionRepository;
import com.ghostkitchen.repository.DiscrepancyRepository;
import com.ghostkitchen.repository.OrganizationRepository;
import com.ghostkitchen.repository.PlatformRepository;
import com.ghostkitchen.repository.PlatformTransactionRepository;
import com.ghostkitchen.repository.ReconciliationResultRepository;
import com.ghostkitchen.repository.ReconciliationRunRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ReconciliationRunService {

    private final ReconciliationRunRepository runRepository;
    private final ReconciliationResultRepository resultRepository;
    private final DiscrepancyRepository discrepancyRepository;
    private final PlatformTransactionRepository platformTransactionRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final OrganizationRepository organizationRepository;
    private final PlatformRepository platformRepository;
    private final ReconciliationEngine engine;

    public ReconciliationRunService(ReconciliationRunRepository runRepository,
                                     ReconciliationResultRepository resultRepository,
                                     DiscrepancyRepository discrepancyRepository,
                                     PlatformTransactionRepository platformTransactionRepository,
                                     BankTransactionRepository bankTransactionRepository,
                                     OrganizationRepository organizationRepository,
                                     PlatformRepository platformRepository,
                                     ReconciliationEngine engine) {
        this.runRepository = runRepository;
        this.resultRepository = resultRepository;
        this.discrepancyRepository = discrepancyRepository;
        this.platformTransactionRepository = platformTransactionRepository;
        this.bankTransactionRepository = bankTransactionRepository;
        this.organizationRepository = organizationRepository;
        this.platformRepository = platformRepository;
        this.engine = engine;
    }

    @Transactional
    public ReconciliationRunResponse triggerRun(UUID organizationId, TriggerReconciliationRequest request) {
        if (request.periodEnd().isBefore(request.periodStart())) {
            throw new InvalidRequestException("periodEnd must not be before periodStart");
        }

        Organization organization = organizationRepository.getReferenceById(organizationId);
        Platform platform = platformRepository.findById(request.platformId())
                .orElseThrow(() -> new ResourceNotFoundException("Platform not found: " + request.platformId()));

        ReconciliationRun run = new ReconciliationRun(organization, platform, request.periodStart(), request.periodEnd());
        runRepository.save(run);

        ReconciliationConfig config = ReconciliationConfig.defaults();

        List<PlatformTransaction> platformTransactions = platformTransactionRepository.findForReconciliation(
                organizationId, request.platformId(), request.periodStart(), request.periodEnd());

        // Don't re-process an order a prior run already resolved.
        Set<UUID> alreadyReconciled = new HashSet<>(resultRepository.findReconciledPlatformTransactionIds(organizationId));
        platformTransactions = platformTransactions.stream()
                .filter(pt -> !alreadyReconciled.contains(pt.getId()))
                .toList();

        LocalDate fromDate = request.periodStart().minusDays(config.dateWindowDays());
        LocalDate toDate = request.periodEnd().plusDays(config.dateWindowDays());
        List<BankTransaction> bankTransactions =
                bankTransactionRepository.findUnmatchedCandidates(organizationId, fromDate, toDate);

        List<ReconciliationOutcome> outcomes = engine.reconcile(run, platformTransactions, bankTransactions, config);

        Map<ReconciliationResultStatus, Long> statusCounts = new EnumMap<>(ReconciliationResultStatus.class);
        for (ReconciliationOutcome outcome : outcomes) {
            resultRepository.save(outcome.result());
            discrepancyRepository.saveAll(outcome.discrepancies());
            statusCounts.merge(outcome.result().getStatus(), 1L, Long::sum);
        }
        bankTransactionRepository.saveAll(bankTransactions);

        run.setStatus(ReconciliationRunStatus.COMPLETED);
        runRepository.save(run);

        return ReconciliationRunResponse.from(run, outcomes.size(), statusCounts);
    }

    @Transactional(readOnly = true)
    public List<ReconciliationRunResponse> listMine(UUID organizationId) {
        return runRepository.findByOrganization_IdOrderByExecutedAtDesc(organizationId).stream()
                .map(run -> {
                    Map<ReconciliationResultStatus, Long> statusCounts = new EnumMap<>(ReconciliationResultStatus.class);
                    int total = 0;
                    for (ReconciliationResultStatus status : ReconciliationResultStatus.values()) {
                        long count = resultRepository.countByReconciliationRun_IdAndStatus(run.getId(), status);
                        if (count > 0) {
                            statusCounts.put(status, count);
                        }
                        total += count;
                    }
                    return ReconciliationRunResponse.from(run, total, statusCounts);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public ReconciliationRunResponse getOwnedRun(UUID runId, UUID callerOrganizationId) {
        ReconciliationRun run = findOwnedRun(runId, callerOrganizationId);

        Map<ReconciliationResultStatus, Long> statusCounts = new EnumMap<>(ReconciliationResultStatus.class);
        int total = 0;
        for (ReconciliationResultStatus status : ReconciliationResultStatus.values()) {
            long count = resultRepository.countByReconciliationRun_IdAndStatus(runId, status);
            if (count > 0) {
                statusCounts.put(status, count);
            }
            total += count;
        }

        return ReconciliationRunResponse.from(run, total, statusCounts);
    }

    @Transactional(readOnly = true)
    public List<ReconciliationResultResponse> getResults(UUID runId, UUID callerOrganizationId, ReconciliationResultStatus statusFilter) {
        findOwnedRun(runId, callerOrganizationId);

        List<ReconciliationResult> results = statusFilter == null
                ? resultRepository.findByReconciliationRun_IdOrderByCreatedAtAsc(runId)
                : resultRepository.findByReconciliationRun_IdAndStatusOrderByCreatedAtAsc(runId, statusFilter);

        return results.stream().map(ReconciliationResultResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<DiscrepancyResponse> getDiscrepancies(UUID resultId, UUID callerOrganizationId) {
        ReconciliationResult result = resultRepository.findById(resultId)
                .orElseThrow(() -> new ResourceNotFoundException("Reconciliation result not found: " + resultId));

        if (!result.getOrganization().getId().equals(callerOrganizationId)) {
            throw new AccessDeniedException("Reconciliation result " + resultId + " is not accessible to this user");
        }

        return discrepancyRepository.findByReconciliationResult_Id(resultId).stream()
                .map(DiscrepancyResponse::from)
                .toList();
    }

    private ReconciliationRun findOwnedRun(UUID runId, UUID callerOrganizationId) {
        ReconciliationRun run = runRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("Reconciliation run not found: " + runId));

        if (!run.getOrganization().getId().equals(callerOrganizationId)) {
            throw new AccessDeniedException("Reconciliation run " + runId + " is not accessible to this user");
        }
        return run;
    }
}
