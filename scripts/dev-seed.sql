-- Local-dev-only sample data. NOT a Flyway migration — run manually against a
-- dev database once the schema migrations above have been applied, e.g.:
--   psql "$DATABASE_URL" -f scripts/dev-seed.sql
--
-- Replace the password_hash placeholder with a real bcrypt hash before using
-- these credentials to log in (never insert a plaintext password).
WITH new_org AS (
    INSERT INTO organization (name) VALUES ('Sample Cloud Kitchen')
    RETURNING id
)
INSERT INTO app_user (organization_id, email, password_hash, role)
SELECT id, 'owner@samplekitchen.test', '$2a$10$REPLACE_WITH_A_REAL_BCRYPT_HASH', 'OWNER'
FROM new_org;
