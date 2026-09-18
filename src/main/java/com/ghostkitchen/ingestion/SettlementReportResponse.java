package com.ghostkitchen.ingestion;

import com.ghostkitchen.entity.ReportStatus;
import com.ghostkitchen.entity.SettlementReport;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SettlementReportResponse(
        UUID id,
        UUID organizationId,
        String platformCode,
        String platformDisplayName,
        String fileName,
        String fileHash,
        Instant uploadedAt,
        LocalDate periodStart,
        LocalDate periodEnd,
        ReportStatus status,
        String errorDetail,
        long transactionCount
) {
    public static SettlementReportResponse from(SettlementReport report, long transactionCount) {
        return new SettlementReportResponse(
                report.getId(),
                report.getOrganization().getId(),
                report.getPlatform().getCode(),
                report.getPlatform().getDisplayName(),
                report.getFileName(),
                report.getFileHash(),
                report.getUploadedAt(),
                report.getPeriodStart(),
                report.getPeriodEnd(),
                report.getStatus(),
                report.getErrorDetail(),
                transactionCount);
    }
}
