package com.ghostkitchen.reconciliation;

import com.ghostkitchen.entity.BankTransaction;
import com.ghostkitchen.entity.Discrepancy;
import com.ghostkitchen.entity.DiscrepancyCategory;
import com.ghostkitchen.entity.Organization;
import com.ghostkitchen.entity.PlatformTransaction;
import com.ghostkitchen.entity.ReconciliationResult;
import com.ghostkitchen.entity.ReconciliationResultStatus;
import com.ghostkitchen.entity.ReconciliationRun;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Deterministic, rule-based matching — no AI/LLM involved anywhere in this class, by design
 * (a reconciliation verdict must be reproducible and auditable, never a model's best guess).
 *
 * <p><b>Scope decision:</b> this matches one platform_transaction to at most one
 * bank_transaction (1:1), not the many-platform-transactions-to-one-bank-transaction batching
 * ARCHITECTURE.md Section 11 describes as a real possibility. That batching assumption is
 * explicitly flagged there as unverified pending real Swiggy/Zomato sample data, and the
 * reconciliation_result schema (a single nullable platform_transaction_id / bank_transaction_id
 * per row) only cleanly supports 1:1 anyway. Revisit once real settlement data shows whether
 * batching is actually needed.
 *
 * <p>Callers must pass only platformTransactions/bankTransactions belonging to
 * run.getOrganization() for the run's period — this method does not re-filter by organization
 * or date range itself.
 */
@Component
public class ReconciliationEngine {

    public List<ReconciliationOutcome> reconcile(ReconciliationRun run,
                                                  List<PlatformTransaction> platformTransactions,
                                                  List<BankTransaction> bankTransactions,
                                                  ReconciliationConfig config) {
        Organization organization = run.getOrganization();

        List<PlatformTransaction> sortedTransactions = platformTransactions.stream()
                .sorted(Comparator.comparing(PlatformTransaction::getOrderDate)
                        .thenComparing(PlatformTransaction::getPlatformOrderId))
                .toList();

        List<ReconciliationOutcome> outcomes = new ArrayList<>();

        for (PlatformTransaction platformTransaction : sortedTransactions) {
            Optional<BankTransaction> candidate = findBestCandidate(platformTransaction, bankTransactions, config);

            if (candidate.isEmpty()) {
                outcomes.add(missingOutcome(run, organization, platformTransaction));
            } else {
                BankTransaction bankTransaction = candidate.get();
                bankTransaction.setMatched(true);
                outcomes.add(matchedOrDiscrepancyOutcome(run, organization, platformTransaction, bankTransaction, config));
            }
        }

        for (BankTransaction bankTransaction : bankTransactions) {
            if (!bankTransaction.isMatched()) {
                outcomes.add(unexplainedOutcome(run, organization, bankTransaction));
            }
        }

        return outcomes;
    }

    /**
     * Among unclaimed bank transactions within the date window, picks the one whose amount is
     * closest to the order's expected payout — ties broken by earliest txn date, then by
     * position in the input list. There is no cap on how far off that closest amount may be;
     * only whether at least one candidate exists in the date window decides MISSING vs. found
     * (found then classifies as MATCHED/UNDERPAID/OVERPAID by the amount tolerance).
     */
    private Optional<BankTransaction> findBestCandidate(PlatformTransaction platformTransaction,
                                                          List<BankTransaction> bankTransactions,
                                                          ReconciliationConfig config) {
        return bankTransactions.stream()
                .filter(bt -> !bt.isMatched())
                .filter(bt -> withinDateWindow(platformTransaction, bt, config.dateWindowDays()))
                .min(Comparator
                        .comparing((BankTransaction bt) -> amountDistance(platformTransaction.getNetExpectedPayout(), bt.getAmount()))
                        .thenComparing(BankTransaction::getTxnDate));
    }

    private boolean withinDateWindow(PlatformTransaction platformTransaction, BankTransaction bankTransaction, int windowDays) {
        long days = Math.abs(ChronoUnit.DAYS.between(platformTransaction.getOrderDate(), bankTransaction.getTxnDate()));
        return days <= windowDays;
    }

    private BigDecimal amountDistance(BigDecimal expected, BigDecimal actual) {
        return expected.subtract(actual).abs();
    }

    private ReconciliationOutcome matchedOrDiscrepancyOutcome(ReconciliationRun run, Organization organization,
                                                                PlatformTransaction platformTransaction,
                                                                BankTransaction bankTransaction,
                                                                ReconciliationConfig config) {
        BigDecimal expected = platformTransaction.getNetExpectedPayout();
        BigDecimal actual = bankTransaction.getAmount();
        BigDecimal difference = actual.subtract(expected);

        ReconciliationResultStatus status;
        if (difference.abs().compareTo(config.amountTolerance()) <= 0) {
            status = ReconciliationResultStatus.MATCHED;
        } else if (difference.signum() < 0) {
            status = ReconciliationResultStatus.UNDERPAID;
        } else {
            status = ReconciliationResultStatus.OVERPAID;
        }

        ReconciliationResult result = new ReconciliationResult(run, organization, status);
        result.setPlatformTransaction(platformTransaction);
        result.setBankTransaction(bankTransaction);
        result.setExpectedAmount(expected);
        result.setActualAmount(actual);
        result.setDifference(difference);
        result.setExplanation(explain(status, expected, actual, difference));

        List<Discrepancy> discrepancies = status == ReconciliationResultStatus.MATCHED
                ? List.of()
                : List.of(aggregateDiscrepancy(result, organization, expected, actual, difference));

        return new ReconciliationOutcome(result, discrepancies);
    }

    private ReconciliationOutcome missingOutcome(ReconciliationRun run, Organization organization,
                                                  PlatformTransaction platformTransaction) {
        BigDecimal expected = platformTransaction.getNetExpectedPayout();
        BigDecimal actual = BigDecimal.ZERO.setScale(2);
        BigDecimal difference = actual.subtract(expected);

        ReconciliationResult result = new ReconciliationResult(run, organization, ReconciliationResultStatus.MISSING);
        result.setPlatformTransaction(platformTransaction);
        result.setExpectedAmount(expected);
        result.setActualAmount(actual);
        result.setDifference(difference);
        result.setExplanation(explain(ReconciliationResultStatus.MISSING, expected, actual, difference));

        Discrepancy discrepancy = new Discrepancy(
                result, organization, DiscrepancyCategory.MISSING_TRANSACTION, expected, actual, difference.abs());
        discrepancy.setNotes("No bank transaction was found within the reconciliation window for this order.");

        return new ReconciliationOutcome(result, List.of(discrepancy));
    }

    private ReconciliationOutcome unexplainedOutcome(ReconciliationRun run, Organization organization,
                                                       BankTransaction bankTransaction) {
        BigDecimal expected = BigDecimal.ZERO.setScale(2);
        BigDecimal actual = bankTransaction.getAmount();
        BigDecimal difference = actual.subtract(expected);

        ReconciliationResult result = new ReconciliationResult(run, organization, ReconciliationResultStatus.UNEXPLAINED);
        result.setBankTransaction(bankTransaction);
        result.setExpectedAmount(expected);
        result.setActualAmount(actual);
        result.setDifference(difference);
        result.setExplanation(explain(ReconciliationResultStatus.UNEXPLAINED, expected, actual, difference));

        Discrepancy discrepancy = new Discrepancy(
                result, organization, DiscrepancyCategory.UNKNOWN, expected, actual, difference.abs());
        discrepancy.setNotes("This bank transaction has no corresponding platform order in the reconciliation window.");

        return new ReconciliationOutcome(result, List.of(discrepancy));
    }

    /**
     * We only know the declared net payout and what the bank actually paid — not which
     * individual deduction category (commission, ad fee, ...) caused the gap, since that would
     * require the platform's own itemized ledger to compare against. Recording that as UNKNOWN
     * rather than guessing a category is the deliberate choice — see ARCHITECTURE.md Section 2,
     * "no invented platform rules".
     */
    private Discrepancy aggregateDiscrepancy(ReconciliationResult result, Organization organization,
                                              BigDecimal expected, BigDecimal actual, BigDecimal difference) {
        Discrepancy discrepancy = new Discrepancy(
                result, organization, DiscrepancyCategory.UNKNOWN, expected, actual, difference.abs());
        discrepancy.setNotes("Aggregate gap between declared net payout and bank amount; category-level "
                + "attribution requires the platform's own itemized deduction ledger, not yet available.");
        return discrepancy;
    }

    private String explain(ReconciliationResultStatus status, BigDecimal expected, BigDecimal actual, BigDecimal difference) {
        return switch (status) {
            case MATCHED -> "Bank amount %s matches expected payout %s within tolerance.".formatted(actual, expected);
            case UNDERPAID -> "Bank amount %s is %s less than expected payout %s.".formatted(actual, difference.abs(), expected);
            case OVERPAID -> "Bank amount %s is %s more than expected payout %s.".formatted(actual, difference.abs(), expected);
            case MISSING -> "Expected payout %s was never matched to a bank transaction within the reconciliation window.".formatted(expected);
            case UNEXPLAINED -> "Bank transaction of %s has no corresponding platform transaction.".formatted(actual);
        };
    }
}
