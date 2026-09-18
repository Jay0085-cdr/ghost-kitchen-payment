CREATE TABLE bank_transaction (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bank_statement_id  UUID NOT NULL REFERENCES bank_statement(id) ON DELETE CASCADE,
    organization_id    UUID NOT NULL REFERENCES organization(id) ON DELETE RESTRICT,
    txn_date           DATE NOT NULL,
    amount             NUMERIC(12,2) NOT NULL,
    narration          TEXT,
    reference_no       VARCHAR(100),
    matched            BOOLEAN NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_bank_transaction_org ON bank_transaction(organization_id);
CREATE INDEX idx_bank_transaction_statement ON bank_transaction(bank_statement_id);
CREATE INDEX idx_bank_transaction_date ON bank_transaction(txn_date);
-- Speeds up the reconciliation engine's candidate search (ARCHITECTURE.md Section 7 step 2),
-- which only ever looks at unmatched transactions.
CREATE INDEX idx_bank_transaction_unmatched ON bank_transaction(organization_id, txn_date) WHERE matched = FALSE;
