package com.ghostkitchen.ingestion.adapter;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Best-effort fallback for any platform without a dedicated adapter — including Swiggy/Zomato
 * until real sample reports let us build those (Phase 10). It invents no platform-specific
 * deduction rules; it only maps whatever deduction columns are actually present in the
 * uploaded CSV under common header names, so it proves the ingestion pipeline end-to-end
 * without pretending to know a real platform's fee structure.
 *
 * Not a real platform: PlatformAdapterRegistry uses this as the fallback when the requested
 * platform.code has no dedicated adapter, not something callers look up directly.
 */
@Component
public class GenericCsvAdapter implements SettlementReportAdapter {

    private static final String PLATFORM_CODE = "GENERIC_CSV";

    private static final List<String> ORDER_ID = List.of("platform_order_id", "order_id", "order id", "id");
    private static final List<String> ORDER_DATE = List.of("order_date", "date");
    private static final List<String> GROSS_AMOUNT = List.of("gross_amount", "gross", "amount");
    private static final List<String> COMMISSION = List.of("commission");
    private static final List<String> ADVERTISING_FEE = List.of("advertising_fee", "ad_fee", "advertising");
    private static final List<String> CANCELLATION_PENALTY = List.of("cancellation_penalty", "cancellation");
    private static final List<String> TAX_ADJUSTMENT = List.of("tax_adjustment", "tax");
    private static final List<String> OTHER_DEDUCTION = List.of("other_deduction", "other", "misc_deduction");
    private static final List<String> NET_EXPECTED_PAYOUT = List.of("net_expected_payout", "net_payout", "payout", "net");

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
            BigDecimal commission = CsvSupport.optionalAmount(row, COMMISSION, lineNumber);
            BigDecimal advertisingFee = CsvSupport.optionalAmount(row, ADVERTISING_FEE, lineNumber);
            BigDecimal cancellationPenalty = CsvSupport.optionalAmount(row, CANCELLATION_PENALTY, lineNumber);
            BigDecimal taxAdjustment = CsvSupport.optionalAmount(row, TAX_ADJUSTMENT, lineNumber);
            BigDecimal otherDeduction = CsvSupport.optionalAmount(row, OTHER_DEDUCTION, lineNumber);

            Optional<String> explicitNet = CsvSupport.findColumn(row, NET_EXPECTED_PAYOUT);
            BigDecimal net = explicitNet.isPresent()
                    ? CsvSupport.parseAmount(explicitNet.get(), lineNumber)
                    : gross.subtract(commission).subtract(advertisingFee).subtract(cancellationPenalty)
                          .subtract(taxAdjustment).subtract(otherDeduction);

            result.add(new ParsedTransaction(
                    orderId, orderDate, gross, commission, advertisingFee, cancellationPenalty,
                    taxAdjustment, otherDeduction, net, CsvSupport.toJson(row)));
        }
        return result;
    }
}
