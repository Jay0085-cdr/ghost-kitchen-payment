package com.ghostkitchen.ingestion;

import com.ghostkitchen.entity.BankStatement;
import com.ghostkitchen.entity.ReportStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BankStatementResponse(
        UUID id,
        UUID organizationId,
        String fileName,
        String fileHash,
        Instant uploadedAt,
        ReportStatus status,
        String errorDetail,
        List<BankTransactionResponse> transactions
) {
    public static BankStatementResponse from(BankStatement statement, List<BankTransactionResponse> transactions) {
        return new BankStatementResponse(
                statement.getId(),
                statement.getOrganization().getId(),
                statement.getFileName(),
                statement.getFileHash(),
                statement.getUploadedAt(),
                statement.getStatus(),
                statement.getErrorDetail(),
                transactions);
    }
}
