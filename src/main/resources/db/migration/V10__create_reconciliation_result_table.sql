CREATE TABLE reconciliation_result (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reconciliation_run_id   UUID NOT NULL REFERENCES reconciliation_run(id) ON DELETE CASCADE,
    organization_id         UUID NOT NULL REFERENCES organization(id) ON DELETE RESTRICT,
    -- Nullable: a MISSING result has no bank_transaction, an UNEXPLAINED result has no
    -- platform_transaction. RESTRICT (not SET NULL): a transaction that has already been
    -- reconciled must never be deletable out from under its result — see ARCHITECTURE.md
    -- Section 2, "every result must be traceable back to source data".
    platform_transaction_id UUID REFERENCES platform_transaction(id) ON DELETE RESTRICT,
    bank_transaction_id     UUID REFERENCES bank_transaction(id) ON DELETE RESTRICT,
    expected_amount         NUMERIC(12,2),
    actual_amount           NUMERIC(12,2),
    difference              NUMERIC(12,2),
    status                  VARCHAR(20) NOT NULL
                                CHECK (status IN ('MATCHED', 'UNDERPAID', 'OVERPAID', 'MISSING', 'UNEXPLAINED')),
    explanation             TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_reconciliation_result_has_a_side
        CHECK (platform_transaction_id IS NOT NULL OR bank_transaction_id IS NOT NULL)
);

CREATE INDEX idx_reconciliation_result_run ON reconciliation_result(reconciliation_run_id);
CREATE INDEX idx_reconciliation_result_org ON reconciliation_result(organization_id);
CREATE INDEX idx_reconciliation_result_status ON reconciliation_result(status);
CREATE INDEX idx_reconciliation_result_platform_txn ON reconciliation_result(platform_transaction_id);
CREATE INDEX idx_reconciliation_result_bank_txn ON reconciliation_result(bank_transaction_id);
