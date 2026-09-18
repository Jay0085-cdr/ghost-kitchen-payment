package com.ghostkitchen.ingestion;

import com.ghostkitchen.entity.BankTransaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BankTransactionResponse(
        UUID id,
        LocalDate txnDate,
        BigDecimal amount,
        String narration,
        String referenceNo,
        boolean matched
) {
    public static BankTransactionResponse from(BankTransaction transaction) {
        return new BankTransactionResponse(
                transaction.getId(),
                transaction.getTxnDate(),
                transaction.getAmount(),
                transaction.getNarration(),
                transaction.getReferenceNo(),
                transaction.isMatched());
    }
}
