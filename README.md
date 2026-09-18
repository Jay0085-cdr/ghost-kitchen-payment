# Ghost Kitchen — Build Reference

Simple reference for what's actually been built so far. For full design
details see `ARCHITECTURE.md`; for phase status/history see `ROADMAP.md`.
This file is just a plain-English summary of Phases 2–5.

**Status: nothing has been run yet.** All of this is written but unverified
— no Java/Maven/Postgres has been available to actually compile or run it.

---

## The one-line pitch

A restaurant sells through Swiggy, Zomato, and direct orders. Each platform
sends a settlement report (gross sales minus commission/fees) and then pays
out a lump sum to the bank. This app checks whether the bank payout actually
matches what the platform said it owed — and flags it when it doesn't.

---

## Phase 2 — Database Schema

**Where:** `src/main/resources/db/migration/V1__...sql` through `V12__...sql`

10 Postgres tables, applied in order by Flyway:

| Table | What it is |
|---|---|
| `organization` | One restaurant (the tenant) |
| `app_user` | A login, belongs to one organization, role OWNER or STAFF |
| `platform` | A sales channel name — Swiggy, Zomato, Direct |
| `settlement_report` | One uploaded platform settlement file |
| `platform_transaction` | One order's line-item from a settlement report |
| `bank_statement` | One uploaded bank statement file |
| `bank_transaction` | One line from a bank statement |
| `reconciliation_run` | One "check this period" execution |
| `reconciliation_result` | The verdict for one order/payout pair |
| `discrepancy` | The breakdown of *why* a result was off |

Every business table has an `organization_id` column so one restaurant's
data is never mixed with another's. Money is always `NUMERIC(12,2)`, never a
float.

---

## Phase 3 — Backend Scaffolding & Auth

**Where:** `pom.xml`, `src/main/java/com/ghostkitchen/`

- A Java class for each of the 10 tables above (`entity/`), plus a
  repository interface for each (`repository/`) so the app can read/write them.
- `POST /api/auth/register` — creates a brand-new organization with you as
  its OWNER, and logs you in.
- `POST /api/auth/login` — checks your password, hands back a JWT (a signed
  login token).
- Every request after that proves who you are via that JWT — your
  organization id comes from the token, never from anything you type into a
  URL.
- `GET /api/organizations/{id}` — a test endpoint proving the above: asking
  for another organization's id gets rejected (403), not silently allowed.
- One shared error format for every kind of failure (bad input, not found,
  no permission, server error) — never a raw crash dumped to the screen.

---

## Phase 4 — Ingestion (file uploads)

**Where:** `src/main/java/com/ghostkitchen/ingestion/`

- `POST /api/settlement-reports/upload` — upload a platform's CSV, it gets
  turned into `platform_transaction` rows.
- `POST /api/bank-statements/upload` — upload a bank statement CSV, it gets
  turned into `bank_transaction` rows.
- Two adapters read the CSV: `DirectOrderAdapter` (our own fixed format for
  direct orders) and `GenericCsvAdapter` (a flexible fallback for any
  platform, including Swiggy/Zomato — we don't have real sample files from
  them yet, so there's no dedicated adapter for them yet).
- Uploading the exact same file twice is rejected — it's fingerprinted by a
  hash, so nothing gets double-counted.
- A bad/malformed file doesn't crash anything — the upload is saved with a
  `FAILED` status and a plain-English reason.
- **Not done yet:** the original uploaded file itself isn't stored anywhere
  — only the hash and the parsed rows survive.

---

## Phase 5 — Reconciliation Engine

**Where:** `src/main/java/com/ghostkitchen/reconciliation/`

The actual matching logic — plain rule-based Java, no AI involved in the
math anywhere.

For every order (`platform_transaction`), it looks for the closest bank
payout within 7 days and ₹1 of the expected amount, and calls it one of:

- **MATCHED** — amounts line up
- **UNDERPAID** — bank paid less than expected
- **OVERPAID** — bank paid more than expected
- **MISSING** — no bank payout ever showed up for this order
- **UNEXPLAINED** — a bank payout exists with no matching order

A bank payout can never be claimed by two orders — that's tested explicitly.

**Not done yet:** this logic exists but nothing calls it — there's no
"run reconciliation" button/endpoint, and no results get saved to the
database. That's Phase 6.

**One open question, called out in the code:** the engine currently matches
one order to one payout. Real settlements might actually bundle many orders
into a single bank payout — we won't know for sure until we see a real
Swiggy/Zomato report, so this is deliberately left simple for now.

---

## What's next

- **Phase 6** — wire the reconciliation engine up to a real endpoint and
  save results to the database.
- **Before that** — actually run Phases 2–5 against a real Postgres to
  confirm all of the above works, not just compiles by inspection.

---

## Folder map

```
ARCHITECTURE.md               full design doc
ROADMAP.md                    phase-by-phase status/history
README.md                     this file
pom.xml                       Java project + dependencies

src/main/resources/
  application.yml             app config (DB, JWT, upload limits)
  db/migration/                Phase 2 — SQL schema
scripts/dev-seed.sql          optional sample org+user for local testing

src/main/java/com/ghostkitchen/
  entity/                     Phase 3 — one class per DB table
  repository/                 Phase 3 — DB read/write for each table
  security/, config/          Phase 3 — JWT + login
  auth/, organization/        Phase 3 — register/login/org endpoints
  exception/                  Phase 3 — shared error handling
  ingestion/                  Phase 4 — CSV upload + parsing
  reconciliation/             Phase 5 — matching engine

src/test/java/com/ghostkitchen/
  reconciliation/             Phase 5 — unit tests
```
