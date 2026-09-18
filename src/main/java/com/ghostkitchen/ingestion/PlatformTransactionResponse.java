package com.ghostkitchen.ingestion;

import com.ghostkitchen.entity.PlatformTransaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PlatformTransactionResponse(
        UUID id,
        String platformOrderId,
        LocalDate orderDate,
        BigDecimal grossAmount,
        BigDecimal commission,
        BigDecimal advertisingFee,
        BigDecimal cancellationPenalty,
        BigDecimal taxAdjustment,
        BigDecimal otherDeduction,
        BigDecimal netExpectedPayout
) {
    public static PlatformTransactionResponse from(PlatformTransaction transaction) {
        return new PlatformTransactionResponse(
                transaction.getId(),
                transaction.getPlatformOrderId(),
                transaction.getOrderDate(),
                transaction.getGrossAmount(),
                transaction.getCommission(),
                transaction.getAdvertisingFee(),
                transaction.getCancellationPenalty(),
                transaction.getTaxAdjustment(),
                transaction.getOtherDeduction(),
                transaction.getNetExpectedPayout());
    }
}
