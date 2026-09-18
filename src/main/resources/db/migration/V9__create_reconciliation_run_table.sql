CREATE TABLE reconciliation_run (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id  UUID NOT NULL REFERENCES organization(id) ON DELETE RESTRICT,
    platform_id      UUID NOT NULL REFERENCES platform(id) ON DELETE RESTRICT,
    period_start     DATE NOT NULL,
    period_end       DATE NOT NULL,
    executed_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    status           VARCHAR(20) NOT NULL DEFAULT 'RUNNING'
                         CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED')),
    CONSTRAINT ck_reconciliation_run_period CHECK (period_end >= period_start)
);

CREATE INDEX idx_reconciliation_run_org_platform_period ON reconciliation_run(organization_id, platform_id, period_start, period_end);
