package com.ghostkitchen.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** The common normalized model every SettlementReportAdapter must produce. */
@Entity
@Table(name = "platform_transaction")
public class PlatformTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "settlement_report_id", nullable = false)
    private SettlementReport settlementReport;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "platform_order_id", nullable = false, length = 100)
    private String platformOrderId;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "gross_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal grossAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal commission = BigDecimal.ZERO;

    @Column(name = "advertising_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal advertisingFee = BigDecimal.ZERO;

    @Column(name = "cancellation_penalty", nullable = false, precision = 12, scale = 2)
    private BigDecimal cancellationPenalty = BigDecimal.ZERO;

    @Column(name = "tax_adjustment", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxAdjustment = BigDecimal.ZERO;

    @Column(name = "other_deduction", nullable = false, precision = 12, scale = 2)
    private BigDecimal otherDeduction = BigDecimal.ZERO;

    @Column(name = "net_expected_payout", nullable = false, precision = 12, scale = 2)
    private BigDecimal netExpectedPayout;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_line_ref", columnDefinition = "jsonb")
    private String rawLineRef;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PlatformTransaction() {
    }

    public PlatformTransaction(SettlementReport settlementReport, Organization organization,
                                String platformOrderId, LocalDate orderDate,
                                BigDecimal grossAmount, BigDecimal netExpectedPayout) {
        this.settlementReport = settlementReport;
        this.organization = organization;
        this.platformOrderId = platformOrderId;
        this.orderDate = orderDate;
        this.grossAmount = grossAmount;
        this.netExpectedPayout = netExpectedPayout;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public SettlementReport getSettlementReport() {
        return settlementReport;
    }

    public Organization getOrganization() {
        return organization;
    }

    public String getPlatformOrderId() {
        return platformOrderId;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public BigDecimal getGrossAmount() {
        return grossAmount;
    }

    public BigDecimal getCommission() {
        return commission;
    }

    public void setCommission(BigDecimal commission) {
        this.commission = commission;
    }

    public BigDecimal getAdvertisingFee() {
        return advertisingFee;
    }

    public void setAdvertisingFee(BigDecimal advertisingFee) {
        this.advertisingFee = advertisingFee;
    }

    public BigDecimal getCancellationPenalty() {
        return cancellationPenalty;
    }

    public void setCancellationPenalty(BigDecimal cancellationPenalty) {
        this.cancellationPenalty = cancellationPenalty;
    }

    public BigDecimal getTaxAdjustment() {
        return taxAdjustment;
    }

    public void setTaxAdjustment(BigDecimal taxAdjustment) {
        this.taxAdjustment = taxAdjustment;
    }

    public BigDecimal getOtherDeduction() {
        return otherDeduction;
    }

    public void setOtherDeduction(BigDecimal otherDeduction) {
        this.otherDeduction = otherDeduction;
    }

    public BigDecimal getNetExpectedPayout() {
        return netExpectedPayout;
    }

    public String getRawLineRef() {
        return rawLineRef;
    }

    public void setRawLineRef(String rawLineRef) {
        this.rawLineRef = rawLineRef;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
