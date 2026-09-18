-- status/error_detail mirror settlement_report: bank statements go through the
-- same upload -> parse -> PARSED/FAILED flow described in ARCHITECTURE.md Section 9.
CREATE TABLE bank_statement (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id  UUID NOT NULL REFERENCES organization(id) ON DELETE RESTRICT,
    file_name        VARCHAR(255) NOT NULL,
    file_hash        VARCHAR(64) NOT NULL,
    uploaded_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                         CHECK (status IN ('PENDING', 'PARSED', 'FAILED')),
    error_detail     TEXT,
    CONSTRAINT uq_bank_statement_upload UNIQUE (organization_id, file_hash)
);

CREATE INDEX idx_bank_statement_org ON bank_statement(organization_id);
