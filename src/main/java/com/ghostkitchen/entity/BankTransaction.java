package com.ghostkitchen.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "bank_transaction")
public class BankTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bank_statement_id", nullable = false)
    private BankStatement bankStatement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "txn_date", nullable = false)
    private LocalDate txnDate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    private String narration;

    @Column(name = "reference_no", length = 100)
    private String referenceNo;

    @Column(nullable = false)
    private boolean matched = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected BankTransaction() {
    }

    public BankTransaction(BankStatement bankStatement, Organization organization,
                            LocalDate txnDate, BigDecimal amount) {
        this.bankStatement = bankStatement;
        this.organization = organization;
        this.txnDate = txnDate;
        this.amount = amount;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public BankStatement getBankStatement() {
        return bankStatement;
    }

    public Organization getOrganization() {
        return organization;
    }

    public LocalDate getTxnDate() {
        return txnDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getNarration() {
        return narration;
    }

    public void setNarration(String narration) {
        this.narration = narration;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public boolean isMatched() {
        return matched;
    }

    public void setMatched(boolean matched) {
        this.matched = matched;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
