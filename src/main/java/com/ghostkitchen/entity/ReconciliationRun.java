package com.ghostkitchen.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "reconciliation_run")
public class ReconciliationRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "platform_id", nullable = false)
    private Platform platform;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "executed_at", nullable = false, updatable = false)
    private Instant executedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReconciliationRunStatus status = ReconciliationRunStatus.RUNNING;

    protected ReconciliationRun() {
    }

    public ReconciliationRun(Organization organization, Platform platform,
                              LocalDate periodStart, LocalDate periodEnd) {
        this.organization = organization;
        this.platform = platform;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
    }

    @PrePersist
    void onCreate() {
        executedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public Platform getPlatform() {
        return platform;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }

    public ReconciliationRunStatus getStatus() {
        return status;
    }

    public void setStatus(ReconciliationRunStatus status) {
        this.status = status;
    }
}
