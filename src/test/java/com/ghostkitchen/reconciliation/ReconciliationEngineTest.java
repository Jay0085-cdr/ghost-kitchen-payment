package com.ghostkitchen.reconciliation;

import com.ghostkitchen.entity.BankStatement;
import com.ghostkitchen.entity.BankTransaction;
import com.ghostkitchen.entity.Discrepancy;
import com.ghostkitchen.entity.DiscrepancyCategory;
import com.ghostkitchen.entity.Organization;
import com.ghostkitchen.entity.Platform;
import com.ghostkitchen.entity.PlatformTransaction;
import com.ghostkitchen.entity.ReconciliationResult;
import com.ghostkitchen.entity.ReconciliationResultStatus;
import com.ghostkitchen.entity.ReconciliationRun;
import com.ghostkitchen.entity.SettlementReport;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests — plain dummy entities, no Spring context, no database. All money assertions
 * use isEqualByComparingTo since BigDecimal scale can legitimately differ (e.g. "100" vs
 * "100.00") without the values being unequal.
 */
class ReconciliationEngineTest {

    private final ReconciliationEngine engine = new ReconciliationEngine();
    private final Organization organization = new Organization("Test Kitchen");
    private final Platform platform = new Platform("SWIGGY", "Swiggy");
    private final SettlementReport report = new SettlementReport(
            organization, platform, "report.csv", "report-hash",
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
    private final BankStatement statement = new BankStatement(organization, "statement.csv", "statement-hash");
    private final ReconciliationRun run = new ReconciliationRun(
            organization, platform, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

    private PlatformTransaction order(String orderId, LocalDate orderDate, String gross, String net) {
        return new PlatformTransaction(report, organization, orderId, orderDate, new BigDecimal(gross), new BigDecimal(net));
    }

    private BankTransaction payout(LocalDate txnDate, String amount) {
        return new BankTransaction(statement, organization, txnDate, new BigDecimal(amount));
    }

    @Test
    void matched_whenAmountAndDateWithinTolerance() {
        PlatformTransaction pt = order("ORD-1", LocalDate.of(2026, 1, 5), "1000.00", "900.00");
        BankTransaction bt = payout(LocalDate.of(2026, 1, 7), "900.00");

        List<ReconciliationOutcome> outcomes = engine.reconcile(run, List.of(pt), List.of(bt), ReconciliationConfig.defaults());

        assertThat(outcomes).hasSize(1);
        ReconciliationResult result = outcomes.get(0).result();
        assertThat(result.getStatus()).isEqualTo(ReconciliationResultStatus.MATCHED);
        assertThat(result.getPlatformTransaction()).isEqualTo(pt);
        assertThat(result.getBankTransaction()).isEqualTo(bt);
        assertThat(result.getDifference()).isEqualByComparingTo("0.00");
        assertThat(outcomes.get(0).discrepancies()).isEmpty();
        assertThat(bt.isMatched()).isTrue();
    }

    @Test
    void matched_whenAmountWithinToleranceButNotExact() {
        PlatformTransaction pt = order("ORD-1", LocalDate.of(2026, 1, 5), "1000.00", "900.00");
        BankTransaction bt = payout(LocalDate.of(2026, 1, 5), "900.50");

        List<ReconciliationOutcome> outcomes = engine.reconcile(
                run, List.of(pt), List.of(bt), new ReconciliationConfig(7, new BigDecimal("1.00")));

        assertThat(outcomes.get(0).result().getStatus()).isEqualTo(ReconciliationResultStatus.MATCHED);
    }

    @Test
    void underpaid_whenBankAmountLessThanExpectedBeyondTolerance() {
        PlatformTransaction pt = order("ORD-1", LocalDate.of(2026, 1, 5), "1000.00", "900.00");
        BankTransaction bt = payout(LocalDate.of(2026, 1, 5), "895.00");

        List<ReconciliationOutcome> outcomes = engine.reconcile(
                run, List.of(pt), List.of(bt), new ReconciliationConfig(7, new BigDecimal("1.00")));

        ReconciliationResult result = outcomes.get(0).result();
        assertThat(result.getStatus()).isEqualTo(ReconciliationResultStatus.UNDERPAID);
        assertThat(result.getDifference()).isEqualByComparingTo("-5.00");

        List<Discrepancy> discrepancies = outcomes.get(0).discrepancies();
        assertThat(discrepancies).hasSize(1);
        assertThat(discrepancies.get(0).getCategory()).isEqualTo(DiscrepancyCategory.UNKNOWN);
        assertThat(discrepancies.get(0).getDifferenceAmount()).isEqualByComparingTo("5.00");
    }

    @Test
    void overpaid_whenBankAmountMoreThanExpectedBeyondTolerance() {
        PlatformTransaction pt = order("ORD-1", LocalDate.of(2026, 1, 5), "1000.00", "900.00");
        BankTransaction bt = payout(LocalDate.of(2026, 1, 5), "910.00");

        List<ReconciliationOutcome> outcomes = engine.reconcile(
                run, List.of(pt), List.of(bt), new ReconciliationConfig(7, new BigDecimal("1.00")));

        ReconciliationResult result = outcomes.get(0).result();
        assertThat(result.getStatus()).isEqualTo(ReconciliationResultStatus.OVERPAID);
        assertThat(result.getDifference()).isEqualByComparingTo("10.00");
    }

    @Test
    void missing_whenNoBankTransactionWithinDateWindow() {
        PlatformTransaction pt = order("ORD-1", LocalDate.of(2026, 1, 5), "1000.00", "900.00");
        BankTransaction bt = payout(LocalDate.of(2026, 2, 1), "900.00"); // 27 days later, outside a 7-day window

        List<ReconciliationOutcome> outcomes = engine.reconcile(
                run, List.of(pt), List.of(bt), new ReconciliationConfig(7, new BigDecimal("1.00")));

        // The order gets MISSING (no candidate in window); the untouched bank transaction is a
        // separate leftover and comes back as its own UNEXPLAINED outcome.
        assertThat(outcomes).hasSize(2);

        ReconciliationResult missing = outcomes.get(0).result();
        assertThat(missing.getStatus()).isEqualTo(ReconciliationResultStatus.MISSING);
        assertThat(missing.getPlatformTransaction()).isEqualTo(pt);
        assertThat(missing.getBankTransaction()).isNull();
        assertThat(missing.getExpectedAmount()).isEqualByComparingTo("900.00");
        assertThat(bt.isMatched()).isFalse();

        ReconciliationResult unexplained = outcomes.get(1).result();
        assertThat(unexplained.getStatus()).isEqualTo(ReconciliationResultStatus.UNEXPLAINED);
        assertThat(unexplained.getBankTransaction()).isEqualTo(bt);
    }

    @Test
    void unexplained_whenBankTransactionHasNoMatchingOrder() {
        BankTransaction bt = payout(LocalDate.of(2026, 1, 10), "500.00");

        List<ReconciliationOutcome> outcomes = engine.reconcile(run, List.of(), List.of(bt), ReconciliationConfig.defaults());

        assertThat(outcomes).hasSize(1);
        ReconciliationResult result = outcomes.get(0).result();
        assertThat(result.getStatus()).isEqualTo(ReconciliationResultStatus.UNEXPLAINED);
        assertThat(result.getPlatformTransaction()).isNull();
        assertThat(result.getBankTransaction()).isEqualTo(bt);
        assertThat(result.getActualAmount()).isEqualByComparingTo("500.00");
    }

    @Test
    void duplicateBankAmounts_secondOrderIsMissing_bankTransactionNotReused() {
        // Two orders expect the exact same payout, but only one bank transaction actually
        // arrived — the engine must not let both orders claim it.
        PlatformTransaction firstOrder = order("ORD-1", LocalDate.of(2026, 1, 5), "1000.00", "900.00");
        PlatformTransaction secondOrder = order("ORD-2", LocalDate.of(2026, 1, 6), "1000.00", "900.00");
        BankTransaction onlyPayout = payout(LocalDate.of(2026, 1, 5), "900.00");

        List<ReconciliationOutcome> outcomes = engine.reconcile(
                run, List.of(firstOrder, secondOrder), List.of(onlyPayout), ReconciliationConfig.defaults());

        assertThat(outcomes).hasSize(2);
        // Sorted by orderDate, so ORD-1 (earlier) is processed first and wins the payout.
        assertThat(outcomes.get(0).result().getStatus()).isEqualTo(ReconciliationResultStatus.MATCHED);
        assertThat(outcomes.get(0).result().getPlatformTransaction()).isEqualTo(firstOrder);
        assertThat(outcomes.get(1).result().getStatus()).isEqualTo(ReconciliationResultStatus.MISSING);
        assertThat(outcomes.get(1).result().getPlatformTransaction()).isEqualTo(secondOrder);
        assertThat(onlyPayout.isMatched()).isTrue();
    }

    @Test
    void duplicateBankTransactions_extraOnePicksUpAsUnexplained() {
        // Two identical bank credits landed, but only one order was ever expected — the second
        // must not be silently dropped or double-matched; it surfaces as UNEXPLAINED.
        PlatformTransaction pt = order("ORD-1", LocalDate.of(2026, 1, 5), "1000.00", "900.00");
        BankTransaction firstPayout = payout(LocalDate.of(2026, 1, 5), "900.00");
        BankTransaction duplicatePayout = payout(LocalDate.of(2026, 1, 5), "900.00");

        List<ReconciliationOutcome> outcomes = engine.reconcile(
                run, List.of(pt), List.of(firstPayout, duplicatePayout), ReconciliationConfig.defaults());

        assertThat(outcomes).hasSize(2);
        assertThat(outcomes.get(0).result().getStatus()).isEqualTo(ReconciliationResultStatus.MATCHED);
        assertThat(outcomes.get(0).result().getBankTransaction()).isEqualTo(firstPayout);
        assertThat(outcomes.get(1).result().getStatus()).isEqualTo(ReconciliationResultStatus.UNEXPLAINED);
        assertThat(outcomes.get(1).result().getBankTransaction()).isEqualTo(duplicatePayout);
    }

    @Test
    void dateWindow_boundaryIsInclusive() {
        PlatformTransaction pt = order("ORD-1", LocalDate.of(2026, 1, 1), "1000.00", "900.00");
        BankTransaction bt = payout(LocalDate.of(2026, 1, 8), "900.00"); // exactly 7 days later

        List<ReconciliationOutcome> outcomes = engine.reconcile(
                run, List.of(pt), List.of(bt), new ReconciliationConfig(7, new BigDecimal("1.00")));

        assertThat(outcomes.get(0).result().getStatus()).isEqualTo(ReconciliationResultStatus.MATCHED);
    }

    @Test
    void canonicalWorkedExample_regressionTest() {
        // Locked-in example from ROADMAP.md Phase 5: 100,000 gross -> 71,000 expected net
        // payout -> 70,200 actually received -> 800 underpaid.
        PlatformTransaction pt = order("ORD-CANON", LocalDate.of(2026, 1, 10), "100000.00", "71000.00");
        BankTransaction bt = payout(LocalDate.of(2026, 1, 12), "70200.00");

        List<ReconciliationOutcome> outcomes = engine.reconcile(run, List.of(pt), List.of(bt), ReconciliationConfig.defaults());

        ReconciliationResult result = outcomes.get(0).result();
        assertThat(result.getStatus()).isEqualTo(ReconciliationResultStatus.UNDERPAID);
        assertThat(result.getExpectedAmount()).isEqualByComparingTo("71000.00");
        assertThat(result.getActualAmount()).isEqualByComparingTo("70200.00");
        assertThat(result.getDifference()).isEqualByComparingTo("-800.00");
        assertThat(outcomes.get(0).discrepancies().get(0).getDifferenceAmount()).isEqualByComparingTo("800.00");
    }
}
