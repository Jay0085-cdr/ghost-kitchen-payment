CREATE TABLE settlement_report (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id  UUID NOT NULL REFERENCES organization(id) ON DELETE RESTRICT,
    platform_id      UUID NOT NULL REFERENCES platform(id) ON DELETE RESTRICT,
    file_name        VARCHAR(255) NOT NULL,
    file_hash        VARCHAR(64) NOT NULL,
    uploaded_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    period_start     DATE NOT NULL,
    period_end       DATE NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                         CHECK (status IN ('PENDING', 'PARSED', 'FAILED')),
    raw_payload      JSONB,
    error_detail     TEXT,
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_settlement_report_period CHECK (period_end >= period_start),
    -- Idempotent re-uploads: same org+platform+period+content is rejected/flagged, never re-ingested.
    CONSTRAINT uq_settlement_report_upload UNIQUE (organization_id, platform_id, period_start, period_end, file_hash)
);

CREATE INDEX idx_settlement_report_org ON settlement_report(organization_id);
CREATE INDEX idx_settlement_report_org_platform_period ON settlement_report(organization_id, platform_id, period_start, period_end);
