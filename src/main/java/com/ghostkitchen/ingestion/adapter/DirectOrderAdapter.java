package com.ghostkitchen.ingestion.adapter;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Direct / WhatsApp orders — the one format we fully control, so its columns are exact
 * rather than guessed. No commission/ad-fee/cancellation-penalty columns exist here because
 * there is no delivery platform taking a cut on a direct order; that's a fact about the
 * source, not an invented deduction rule.
 */
@Component
public class DirectOrderAdapter implements SettlementReportAdapter {

    private static final String PLATFORM_CODE = "DIRECT";

    private static final List<String> ORDER_ID = List.of("order_id");
    private static final List<String> ORDER_DATE = List.of("order_date");
    private static final List<String> GROSS_AMOUNT = List.of("gross_amount");
    private static final List<String> OTHER_DEDUCTION = List.of("other_deduction");

    @Override
    public String supportedPlatformCode() {
        return PLATFORM_CODE;
    }

    @Override
    public List<ParsedTransaction> parse(RawReportFile file) throws ReportParseException {
        List<Map<String, String>> rows = CsvSupport.readAsMaps(file.content());
        if (rows.isEmpty()) {
            throw new ReportParseException("File contains no data rows");
        }

        List<ParsedTransaction> result = new ArrayList<>();
        int lineNumber = 1;
        for (Map<String, String> row : rows) {
            lineNumber++;

            String orderId = CsvSupport.requireColumn(row, ORDER_ID, lineNumber, "order id");
            LocalDate orderDate = CsvSupport.parseDate(CsvSupport.requireColumn(row, ORDER_DATE, lineNumber, "order date"), lineNumber);
            BigDecimal gross = CsvSupport.parseAmount(CsvSupport.requireColumn(row, GROSS_AMOUNT, lineNumber, "gross amount"), lineNumber);
            BigDecimal otherDeduction = CsvSupport.optionalAmount(row, OTHER_DEDUCTION, lineNumber);
            BigDecimal net = gross.subtract(otherDeduction);

            result.add(new ParsedTransaction(
                    orderId, orderDate, gross,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    otherDeduction, net, CsvSupport.toJson(row)));
        }
        return result;
    }
}
