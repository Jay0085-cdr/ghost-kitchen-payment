package com.ghostkitchen.ingestion;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ParsedBankLine(LocalDate txnDate, BigDecimal amount, String narration, String referenceNo) {
}
