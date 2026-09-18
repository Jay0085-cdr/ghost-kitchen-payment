package com.ghostkitchen.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "discrepancy")
public class Discrepancy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reconciliation_result_id", nullable = false)
    private ReconciliationResult reconciliationResult;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DiscrepancyCategory category;

    @Column(name = "expected_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal expectedAmount;

    @Column(name = "actual_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal actualAmount;

    @Column(name = "difference_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal differenceAmount;

    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Discrepancy() {
    }

    public Discrepancy(ReconciliationResult reconciliationResult, Organization organization,
                        DiscrepancyCategory category, BigDecimal expectedAmount,
                        BigDecimal actualAmount, BigDecimal differenceAmount) {
        this.reconciliationResult = reconciliationResult;
        this.organization = organization;
        this.category = category;
        this.expectedAmount = expectedAmount;
        this.actualAmount = actualAmount;
        this.differenceAmount = differenceAmount;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public ReconciliationResult getReconciliationResult() {
        return reconciliationResult;
    }

    public Organization getOrganization() {
        return organization;
    }

    public DiscrepancyCategory getCategory() {
        return category;
    }

    public BigDecimal getExpectedAmount() {
        return expectedAmount;
    }

    public BigDecimal getActualAmount() {
        return actualAmount;
    }

    public BigDecimal getDifferenceAmount() {
        return differenceAmount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
