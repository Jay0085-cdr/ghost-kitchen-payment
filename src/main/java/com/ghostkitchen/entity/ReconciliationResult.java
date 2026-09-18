package com.ghostkitchen.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reconciliation_result")
public class ReconciliationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reconciliation_run_id", nullable = false)
    private ReconciliationRun reconciliationRun;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    /** Nullable: an UNEXPLAINED result has no platform-side transaction. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "platform_transaction_id")
    private PlatformTransaction platformTransaction;

    /** Nullable: a MISSING result has no bank-side transaction. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_transaction_id")
    private BankTransaction bankTransaction;

    @Column(name = "expected_amount", precision = 12, scale = 2)
    private BigDecimal expectedAmount;

    @Column(name = "actual_amount", precision = 12, scale = 2)
    private BigDecimal actualAmount;

    @Column(precision = 12, scale = 2)
    private BigDecimal difference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReconciliationResultStatus status;

    private String explanation;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ReconciliationResult() {
    }

    public ReconciliationResult(ReconciliationRun reconciliationRun, Organization organization,
                                 ReconciliationResultStatus status) {
        this.reconciliationRun = reconciliationRun;
        this.organization = organization;
        this.status = status;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public ReconciliationRun getReconciliationRun() {
        return reconciliationRun;
    }

    public Organization getOrganization() {
        return organization;
    }

    public PlatformTransaction getPlatformTransaction() {
        return platformTransaction;
    }

    public void setPlatformTransaction(PlatformTransaction platformTransaction) {
        this.platformTransaction = platformTransaction;
    }

    public BankTransaction getBankTransaction() {
        return bankTransaction;
    }

    public void setBankTransaction(BankTransaction bankTransaction) {
        this.bankTransaction = bankTransaction;
    }

    public BigDecimal getExpectedAmount() {
        return expectedAmount;
    }

    public void setExpectedAmount(BigDecimal expectedAmount) {
        this.expectedAmount = expectedAmount;
    }

    public BigDecimal getActualAmount() {
        return actualAmount;
    }

    public void setActualAmount(BigDecimal actualAmount) {
        this.actualAmount = actualAmount;
    }

    public BigDecimal getDifference() {
        return difference;
    }

    public void setDifference(BigDecimal difference) {
        this.difference = difference;
    }

    public ReconciliationResultStatus getStatus() {
        return status;
    }

    public void setStatus(ReconciliationResultStatus status) {
        this.status = status;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
