-- The common normalized model every SettlementReportAdapter must produce.
-- One row per order / line-item from a settlement report.
CREATE TABLE platform_transaction (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    settlement_report_id  UUID NOT NULL REFERENCES settlement_report(id) ON DELETE CASCADE,
    organization_id       UUID NOT NULL REFERENCES organization(id) ON DELETE RESTRICT,
    platform_order_id     VARCHAR(100) NOT NULL,
    order_date            DATE NOT NULL,
    gross_amount          NUMERIC(12,2) NOT NULL,
    commission            NUMERIC(12,2) NOT NULL DEFAULT 0,
    advertising_fee       NUMERIC(12,2) NOT NULL DEFAULT 0,
    cancellation_penalty  NUMERIC(12,2) NOT NULL DEFAULT 0,
    tax_adjustment        NUMERIC(12,2) NOT NULL DEFAULT 0,
    other_deduction       NUMERIC(12,2) NOT NULL DEFAULT 0,
    net_expected_payout   NUMERIC(12,2) NOT NULL,
    raw_line_ref          JSONB,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- One row per order per report; prevents duplicate ingestion of the same line item.
    CONSTRAINT uq_platform_transaction_order UNIQUE (settlement_report_id, platform_order_id)
);

CREATE INDEX idx_platform_transaction_org ON platform_transaction(organization_id);
CREATE INDEX idx_platform_transaction_report ON platform_transaction(settlement_report_id);
CREATE INDEX idx_platform_transaction_order_date ON platform_transaction(order_date);
