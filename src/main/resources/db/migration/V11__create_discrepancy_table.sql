-- Normalizes the deduction-category breakdown behind a reconciliation_result's
-- discrepancy (ARCHITECTURE.md Section 7 step 4: "difference is broken down against
-- recorded deduction categories"). One reconciliation_result can have several
-- discrepancy rows, e.g. commission was off AND an ad fee was off on the same order.
CREATE TABLE discrepancy (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reconciliation_result_id  UUID NOT NULL REFERENCES reconciliation_result(id) ON DELETE CASCADE,
    organization_id           UUID NOT NULL REFERENCES organization(id) ON DELETE RESTRICT,
    category                  VARCHAR(30) NOT NULL
                                  CHECK (category IN (
                                      'COMMISSION', 'ADVERTISING_FEE', 'CANCELLATION_PENALTY',
                                      'TAX_ADJUSTMENT', 'OTHER_DEDUCTION', 'MISSING_TRANSACTION', 'UNKNOWN'
                                  )),
    expected_amount           NUMERIC(12,2) NOT NULL,
    actual_amount             NUMERIC(12,2) NOT NULL,
    difference_amount         NUMERIC(12,2) NOT NULL,
    notes                     TEXT,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_discrepancy_result ON discrepancy(reconciliation_result_id);
CREATE INDEX idx_discrepancy_org ON discrepancy(organization_id);
CREATE INDEX idx_discrepancy_category ON discrepancy(category);
