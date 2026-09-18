-- Lookup table for sales channels (Swiggy, Zomato, Direct, ...).
-- A row, not a hardcoded enum, so a new platform never requires a schema change.
-- Shared reference data: intentionally has no organization_id.
CREATE TABLE platform (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code          VARCHAR(30) NOT NULL,
    display_name  VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_platform_code UNIQUE (code)
);
