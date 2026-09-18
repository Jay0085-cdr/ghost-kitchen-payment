package com.ghostkitchen.ingestion;

import com.ghostkitchen.entity.Organization;
import com.ghostkitchen.entity.Platform;
import com.ghostkitchen.entity.PlatformTransaction;
import com.ghostkitchen.entity.ReportStatus;
import com.ghostkitchen.entity.SettlementReport;
import com.ghostkitchen.exception.DuplicateUploadException;
import com.ghostkitchen.exception.InvalidRequestException;
import com.ghostkitchen.exception.ResourceNotFoundException;
import com.ghostkitchen.ingestion.adapter.ParsedTransaction;
import com.ghostkitchen.ingestion.adapter.PlatformAdapterRegistry;
import com.ghostkitchen.ingestion.adapter.RawReportFile;
import com.ghostkitchen.ingestion.adapter.ReportParseException;
import com.ghostkitchen.ingestion.adapter.SettlementReportAdapter;
import com.ghostkitchen.repository.OrganizationRepository;
import com.ghostkitchen.repository.PlatformRepository;
import com.ghostkitchen.repository.PlatformTransactionRepository;
import com.ghostkitchen.repository.SettlementReportRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class SettlementReportService {

    private final SettlementReportRepository settlementReportRepository;
    private final PlatformTransactionRepository platformTransactionRepository;
    private final OrganizationRepository organizationRepository;
    private final PlatformRepository platformRepository;
    private final PlatformAdapterRegistry adapterRegistry;
    private final FileHashService fileHashService;

    public SettlementReportService(SettlementReportRepository settlementReportRepository,
                                    PlatformTransactionRepository platformTransactionRepository,
                                    OrganizationRepository organizationRepository,
                                    PlatformRepository platformRepository,
                                    PlatformAdapterRegistry adapterRegistry,
                                    FileHashService fileHashService) {
        this.settlementReportRepository = settlementReportRepository;
        this.platformTransactionRepository = platformTransactionRepository;
        this.organizationRepository = organizationRepository;
        this.platformRepository = platformRepository;
        this.adapterRegistry = adapterRegistry;
        this.fileHashService = fileHashService;
    }

    /**
     * A parse failure (bad format, missing column, duplicate order id within the file) is a
     * normal outcome, not an error: the report row is still created and saved with status
     * FAILED + error_detail. Only truly unexpected DB-level problems roll the whole thing back
     * (see the duplicate-order-id pre-check below for why we don't rely on catching a DB
     * constraint violation instead — Postgres aborts the whole transaction on the first failed
     * statement, which would take the report row down with it).
     */
    @Transactional
    public SettlementReportResponse upload(UUID organizationId, UUID platformId, LocalDate periodStart,
                                            LocalDate periodEnd, MultipartFile file) {
        if (periodEnd.isBefore(periodStart)) {
            throw new InvalidRequestException("periodEnd must not be before periodStart");
        }
        if (file.isEmpty()) {
            throw new InvalidRequestException("Uploaded file is empty");
        }

        Organization organization = organizationRepository.getReferenceById(organizationId);
        Platform platform = platformRepository.findById(platformId)
                .orElseThrow(() -> new ResourceNotFoundException("Platform not found: " + platformId));

        byte[] content = readBytes(file);
        String fileHash = fileHashService.sha256Hex(content);

        settlementReportRepository
                .findByOrganization_IdAndPlatform_IdAndPeriodStartAndPeriodEndAndFileHash(
                        organizationId, platformId, periodStart, periodEnd, fileHash)
                .ifPresent(existing -> {
                    throw new DuplicateUploadException("settlement report", existing.getId());
                });

        SettlementReport report = new SettlementReport(
                organization, platform, file.getOriginalFilename(), fileHash, periodStart, periodEnd);

        SettlementReportAdapter adapter = adapterRegistry.resolve(platform.getCode());

        try {
            List<ParsedTransaction> parsed = adapter.parse(new RawReportFile(file.getOriginalFilename(), content, file.getContentType()));
            assertNoDuplicateOrderIds(parsed);

            List<PlatformTransaction> transactions = parsed.stream()
                    .map(p -> toEntity(report, organization, p))
                    .toList();

            report.setStatus(ReportStatus.PARSED);
            settlementReportRepository.save(report);
            platformTransactionRepository.saveAll(transactions);

            return SettlementReportResponse.from(report, transactions.size());
        } catch (ReportParseException e) {
            report.setStatus(ReportStatus.FAILED);
            report.setErrorDetail(e.getMessage());
            settlementReportRepository.save(report);
            return SettlementReportResponse.from(report, 0);
        }
    }

    @Transactional(readOnly = true)
    public SettlementReportResponse getOwned(UUID reportId, UUID callerOrganizationId) {
        SettlementReport report = findOwned(reportId, callerOrganizationId);
        long count = platformTransactionRepository.countBySettlementReport_Id(reportId);
        return SettlementReportResponse.from(report, count);
    }

    /** Used by PlatformTransactionController to verify ownership before listing a report's line items. */
    @Transactional(readOnly = true)
    public void assertOwned(UUID reportId, UUID callerOrganizationId) {
        findOwned(reportId, callerOrganizationId);
    }

    @Transactional(readOnly = true)
    public List<SettlementReportResponse> listMine(UUID organizationId) {
        return settlementReportRepository.findByOrganization_IdOrderByUploadedAtDesc(organizationId).stream()
                .map(report -> SettlementReportResponse.from(
                        report, platformTransactionRepository.countBySettlementReport_Id(report.getId())))
                .toList();
    }

    private SettlementReport findOwned(UUID reportId, UUID callerOrganizationId) {
        SettlementReport report = settlementReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Settlement report not found: " + reportId));

        if (!report.getOrganization().getId().equals(callerOrganizationId)) {
            throw new AccessDeniedException("Settlement report " + reportId + " is not accessible to this user");
        }
        return report;
    }

    private void assertNoDuplicateOrderIds(List<ParsedTransaction> parsed) throws ReportParseException {
        Set<String> seen = new HashSet<>();
        for (ParsedTransaction p : parsed) {
            if (!seen.add(p.platformOrderId())) {
                throw new ReportParseException("Duplicate order id within file: " + p.platformOrderId());
            }
        }
    }

    private PlatformTransaction toEntity(SettlementReport report, Organization organization, ParsedTransaction parsed) {
        PlatformTransaction transaction = new PlatformTransaction(
                report, organization, parsed.platformOrderId(), parsed.orderDate(),
                parsed.grossAmount(), parsed.netExpectedPayout());
        transaction.setCommission(parsed.commission());
        transaction.setAdvertisingFee(parsed.advertisingFee());
        transaction.setCancellationPenalty(parsed.cancellationPenalty());
        transaction.setTaxAdjustment(parsed.taxAdjustment());
        transaction.setOtherDeduction(parsed.otherDeduction());
        transaction.setRawLineRef(parsed.rawLineRef());
        return transaction;
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new InvalidRequestException("Could not read uploaded file: " + e.getMessage());
        }
    }
}
