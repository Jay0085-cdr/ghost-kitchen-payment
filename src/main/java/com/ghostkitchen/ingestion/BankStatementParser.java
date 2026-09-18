package com.ghostkitchen.ingestion;

import com.ghostkitchen.ingestion.adapter.CsvSupport;
import com.ghostkitchen.ingestion.adapter.ReportParseException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Only one bank-statement shape is supported for now, so this is a plain parser, not a pluggable adapter. */
@Component
public class BankStatementParser {

    private static final List<String> TXN_DATE = List.of("txn_date", "date");
    private static final List<String> AMOUNT = List.of("amount");
    private static final List<String> NARRATION = List.of("narration", "description");
    private static final List<String> REFERENCE_NO = List.of("reference_no", "reference");

    public List<ParsedBankLine> parse(byte[] content) throws ReportParseException {
        List<Map<String, String>> rows = CsvSupport.readAsMaps(content);
        if (rows.isEmpty()) {
            throw new ReportParseException("File contains no data rows");
        }

        List<ParsedBankLine> result = new ArrayList<>();
        int lineNumber = 1;
        for (Map<String, String> row : rows) {
            lineNumber++;

            LocalDate txnDate = CsvSupport.parseDate(CsvSupport.requireColumn(row, TXN_DATE, lineNumber, "txn date"), lineNumber);
            BigDecimal amount = CsvSupport.parseAmount(CsvSupport.requireColumn(row, AMOUNT, lineNumber, "amount"), lineNumber);
            String narration = CsvSupport.findColumn(row, NARRATION).orElse(null);
            String referenceNo = CsvSupport.findColumn(row, REFERENCE_NO).orElse(null);

            result.add(new ParsedBankLine(txnDate, amount, narration, referenceNo));
        }
        return result;
    }
}
