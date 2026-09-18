# Ghost Kitchen — Project Roadmap

**Restaurant Revenue Intelligence & Reconciliation Platform**
Tracks *what gets built, in what order, and what "done" looks like for each
stage.

Status legend: `DONE` `IN PROGRESS` `NOT STARTED` `BLOCKED`

---

## At a glance

```
Phase 0  Project Setup                         NOT STARTED
Phase 1  Architecture & Technical Design        DONE
Phase 2  Database Schema & Migrations           IN PROGRESS
Phase 3  Backend Scaffolding & Auth             IN PROGRESS
Phase 4  Ingestion & Platform Adapters          IN PROGRESS
Phase 5  Reconciliation Engine                  IN PROGRESS
Phase 6  REST API Layer                         IN PROGRESS
Phase 7  Frontend Dashboard                     IN PROGRESS
Phase 8  Analytics                              NOT STARTED
Phase 9  Testing, Hardening & Deployment        NOT STARTED
Phase 10 Real Platform Adapters (Swiggy/Zomato) BLOCKED on sample data
Phase 11 Future Intelligence (post-MVP)         NOT STARTED
```

MVP = Phases 0–9. Phase 10 is folded in as soon as real sample reports arrive
— it doesn't have to happen last, but it's blocked until then. Phase 11 is
explicitly future work, not part of this build.

---

## Phase 0 — Project Setup
**Status: NOT STARTED**

- Initialize git repository.
- Scaffold Maven multi-module or single-module Spring Boot project structure.
- Scaffold React + Vite + TypeScript frontend project.
- `docker-compose.yml` for local Postgres.
- Base `README.md` (run instructions, project description).

**Done when:** `docker-compose up` gives a running empty Spring Boot app
connected to Postgres, and the React app runs locally against it.

---

## Phase 1 — Architecture & Technical Design
**Status: DONE**

- Problem statement, design principles, tech stack decided.
- Component architecture, domain model, platform adapter abstraction,
  reconciliation algorithm, API surface, and ingestion/error-handling design
  documented in `ARCHITECTURE.md`.

**Output:** `ARCHITECTURE.md`

---

## Phase 2 — Database Schema & Migrations
**Status: IN PROGRESS** — migrations written, not yet run against a live DB
(no Postgres/Docker available in the authoring environment; needs a run
against a real Postgres 16 before this is marked DONE).

- Flyway migration scripts for all entities in `ARCHITECTURE.md` Section 5:
  `organization`, `app_user`, `platform`, `settlement_report`,
  `platform_transaction`, `bank_statement`, `bank_transaction`,
  `reconciliation_run`, `reconciliation_result` — plus a `discrepancy` table
  (normalizes the deduction-category breakdown that Section 5 folded into
  `reconciliation_result.explanation`; not a Section 5 rename, an addition).
  See `src/main/resources/db/migration/V1__...` through `V12__...`.
- Constraints: FK integrity (UUID PKs via `pgcrypto`'s `gen_random_uuid()`),
  `NUMERIC(12,2)` for all money columns, unique constraint on `file_hash` per
  org+platform+period for idempotent uploads, `organization_id` denormalized
  onto every business table for tenant isolation.
- `scripts/dev-seed.sql` inserts a sample organization + owner user (kept
  outside the Flyway chain so it never runs against a real/prod DB).
- Corresponding JPA entity classes + repositories — **not started**, deferred
  to Phase 3 per explicit instruction not to build backend code yet.

**Done when:** migrations run cleanly on a fresh DB, entities map 1:1 to the
documented schema, and a seed script can insert a sample organization +
platform row.

---

## Phase 3 — Backend Scaffolding & Auth
**Status: IN PROGRESS** — code written, not yet compiled or run (no JDK/Maven
available in the authoring environment; needs `mvn spring-boot:run` against a
real Postgres before this is marked DONE).

- Maven project (`pom.xml`, Spring Boot 3.3.4 / Java 21) and JPA entities +
  repositories for all 10 Phase 2 tables (deferred from Phase 2 on purpose).
- Spring Security + stateless JWT auth (`/api/auth/register`,
  `/api/auth/login`) — `jjwt` 0.12.x, HS256. `/register` creates a new
  organization with the caller as its `OWNER` in one step (no "join an
  existing org" flow exists yet, so self-registration = new tenant).
- Role-based access control (`OWNER`, `STAFF`) — granted authorities wired
  through (`ROLE_OWNER`/`ROLE_STAFF`), `@EnableMethodSecurity` on, but no
  endpoint restricts by role yet because no role-specific endpoint exists
  yet to restrict.
- Global `@RestControllerAdvice` exception handler with a consistent JSON
  error shape (validation errors, 401/403/404/409/500 all normalized, no
  stack traces leaked).
- Server-side `organization_id` scoping: the JWT carries `organizationId` as
  a signed claim, `JwtAuthenticationFilter` rebuilds the principal from that
  claim on every request (no DB round trip), and `GET
  /api/organizations/{id}` demonstrates the enforcement — a client-supplied
  id that doesn't match the caller's own organization gets a 403, never
  silently scoped to the wrong tenant.

**Done when:** a user can register, log in, receive a JWT, and hit a
protected test endpoint that correctly rejects requests for another
organization's data.

---

## Phase 4 — Ingestion & Platform Adapters
**Status: IN PROGRESS** — code written, not yet run against a live DB (same
caveat as Phases 2–3: no JDK/Maven/Postgres in the authoring environment).

- `SettlementReportAdapter` interface + `PlatformAdapterRegistry` — one
  deviation from the `ARCHITECTURE.md` §6 sketch, explained in code: `parse()`
  returns a plain `ParsedTransaction` record, not the `platform_transaction`
  JPA entity directly, since the entity needs an already-persisted
  `SettlementReport`/`Organization` the adapter never has. `supportedPlatformCode()`
  returns `platform.code` as a string rather than the `Platform` entity, so an
  adapter needs no DB access to identify itself.
- `DirectOrderAdapter` (fixed columns for the one format we control — no
  commission/ad-fee/penalty columns exist because there genuinely is no
  platform cut on a direct order) and `GenericCsvAdapter` (flexible
  header-alias matching, used as the registry's fallback for any platform —
  including Swiggy/Zomato — that has no dedicated adapter yet; invents no
  deduction rules, just maps whatever columns are actually in the file).
- File upload endpoints (`/api/settlement-reports/upload`,
  `/api/bank-statements/upload`) using OpenCSV. **Apache POI (Excel) is not
  wired in yet** — Phase 4's own done-when criterion only requires CSV, so
  the dependency isn't added until an Excel-parsing adapter actually needs it.
  Also added, since they're necessary to see the result of an upload:
  `GET /api/settlement-reports/{id}`, `GET /api/platform-transactions?reportId=`,
  `GET /api/bank-statements/{id}` (all from the `ARCHITECTURE.md` §8 surface).
- Duplicate-upload detection via `file_hash`, checked in Java before any
  insert is attempted (not by catching a DB constraint violation — Postgres
  aborts the whole transaction on the first failed statement, which would
  take the settlement_report row down with it too).
- Structured parse-failure handling (bad format, missing column, unreadable
  file, duplicate order id within one file) surfaced as a `FAILED` report row
  with `error_detail`, not a stack trace or a lost upload.
- **Not yet wired to a real file store** — the uploaded file's bytes are
  hashed and parsed but not persisted anywhere retrievable after the
  request; only the hash and the derived rows survive. Revisit if Jay needs
  to re-download the original file later.

**Done when:** uploading a sample CSV produces correctly normalized
`platform_transaction` / `bank_transaction` rows, and re-uploading the same
file is rejected/flagged instead of duplicating data.

---

## Phase 5 — Reconciliation Engine
**Status: DONE** — verified 2026-09-18: `mvn test` against JDK 21 / Maven
3.9.16, all 10 `ReconciliationEngineTest` cases pass (BUILD SUCCESS).

- `ReconciliationEngine` (`com.ghostkitchen.reconciliation`) — pure,
  deterministic, rule-based, no AI/LLM anywhere in it. Matches each
  `platform_transaction` to the closest unclaimed `bank_transaction` within a
  configurable date window (default 7 days) and amount tolerance (default
  ₹1), classifying `MATCHED / UNDERPAID / OVERPAID / MISSING`, then any
  bank transactions nobody claimed come back `UNEXPLAINED`.
- **Scope decision, flagged in the class's own doc comment:** matches
  platform transactions to bank transactions **1:1**, not the
  many-orders-to-one-payout batching `ARCHITECTURE.md` Section 11 raises as
  a real possibility. That batching assumption is explicitly marked there as
  unverified pending real Swiggy/Zomato sample data, and the
  `reconciliation_result` schema (one nullable FK each side) only cleanly
  supports 1:1 anyway — revisit once real settlement data arrives.
- Discrepancy breakdown is honest about what we actually know: for
  UNDERPAID/OVERPAID we can't attribute the gap to a specific deduction
  category (commission vs. ad fee vs. ...) without the platform's own
  itemized ledger, so it's recorded as a single `UNKNOWN`-category row
  rather than a guess — consistent with "no invented platform rules."
- 10 unit tests (`ReconciliationEngineTest`, plain JUnit 5 + AssertJ, no
  Spring context, no database — dummy entities built directly) covering all
  five statuses, date-window boundary inclusivity, amount-tolerance boundary,
  a duplicate bank amount competing for one payout (second order correctly
  `MISSING`, payout not reused), a duplicate bank transaction with only one
  order to explain it (extra one correctly `UNEXPLAINED`), and the canonical
  ₹100,000/₹71,000/₹70,200/₹800 worked example as a locked-in regression
  test.
- **Not wired to anything yet, on purpose** (Phase 6 territory, not started):
  no controller, no `reconciliation_run`/`reconciliation_result` persistence,
  nothing querying `platform_transaction`/`bank_transaction` from the DB. The
  engine takes plain lists in and returns plain outcomes out.

**Done when:** running reconciliation against the canonical example produces
the exact expected classification and discrepancy breakdown, and this is
locked in as a regression test.

---

## Phase 6 — REST API Layer
**Status: IN PROGRESS** — core reconciliation endpoints built and compiling
(`mvn compile` verified); never hit with a real HTTP request (see README
"What's actually verified"). OpenAPI/Swagger not added — not requested and
kept out to stay within MVP scope.

- `POST /api/reconciliation/runs` (trigger a run), `GET .../runs`,
  `GET .../runs/{id}`, `GET .../results?runId=&status=`,
  `GET .../results/{id}/discrepancies` — the reconciliation surface from
  `ARCHITECTURE.md` §8, wired to real persistence (the Phase 5 engine's
  output is now saved as `reconciliation_result`/`discrepancy` rows, not
  just computed in memory).
- A run correctly skips orders/bank transactions already resolved by a
  prior run — prevents duplicate results if reconciliation is triggered
  more than once over overlapping data.
- "List mine" endpoints added for settlement reports, bank statements, and
  reconciliation runs (`GET` with no id) — the Phase 4 API only had
  fetch-by-id, which isn't enough for a dashboard to show anything without
  already knowing every id in advance.
- `GET /api/platforms` added — reference-data lookup for populating
  platform dropdowns in the frontend.

**Done when:** every endpoint in the documented API surface is implemented,
authenticated, org-scoped, and visible in the generated Swagger UI.

---

## Phase 7 — Frontend Dashboard
**Status: IN PROGRESS** — built, type-checks and production-builds
(`npm run build` verified); never run against a live backend.

- React + TypeScript + Vite, React Router, no UI framework (plain inline
  styles) — kept deliberately minimal per "simple, functional, not fancy."
- Login/register, Dashboard (org info + status counts across all runs +
  recent run history), Uploads (settlement report + bank statement forms,
  plus lists of what's been uploaded and their parse status), Reconciliation
  (trigger a run, list past runs), Run detail (results table with status
  filter, click a row to see its discrepancy breakdown).
- JWT stored in `localStorage`, attached to every request via a small fetch
  wrapper (`src/api/client.ts`) — fine for an MVP demo, not hardened for
  production (no refresh flow, no XSS-hardening beyond React's own
  escaping).

**Done when:** an owner can, end-to-end in the UI, upload both files, trigger
a reconciliation run, and see a correctly classified, explained result for
every transaction.

---

## Phase 8 — Analytics
**Status: NOT STARTED**

- `/api/analytics/summary` and `/api/analytics/by-platform` endpoints.
- Dashboard views: gross revenue, net revenue, total commission, total
  advertising spend, discrepancy totals — over a selected period.

**Done when:** the dashboard shows accurate aggregate figures that reconcile
against the sum of underlying `platform_transaction` / `reconciliation_result`
rows for the same period.

---

## Phase 9 — Testing, Hardening & Deployment
**Status: NOT STARTED**

- Testcontainers-based integration test suite covering ingestion →
  normalization → reconciliation end-to-end.
- Input validation on all upload/API endpoints.
- Structured logging.
- Dockerized deployment (backend + frontend + Postgres via docker-compose,
  or split into a cloud deployment if desired later).
- README with setup, architecture summary, and a demo walkthrough — this is
  the portfolio-facing polish pass.

**Done when:** a reviewer can clone the repo, run one command, and walk
through the full upload → reconcile → dashboard flow using provided sample
data.

---

## Phase 10 — Real Platform Adapters (Swiggy / Zomato)
**Status: BLOCKED — waiting on anonymized sample settlement reports**

- Obtain redacted/anonymized real settlement reports from the connected
  restaurant-owner contact.
- Derive actual column layouts, deduction categories, and settlement
  granularity (does one bank payout correspond 1:1 to one report period, or
  many:1 — see `ARCHITECTURE.md` Section 11) from real data.
- Implement `SwiggyAdapter`, `ZomatoAdapter` conforming to the existing
  `SettlementReportAdapter` interface — no reconciliation engine changes
  required.
- Validate the reconciliation engine against real anonymized data.

**Done when:** a real (anonymized) Swiggy or Zomato settlement report can be
uploaded and correctly reconciled against a real (anonymized) bank statement,
with no invented deduction rules — every field traced to what the real report
actually contains.

---

## Phase 11 — Future Intelligence (explicitly post-MVP)
**Status: NOT STARTED — not part of this build**

- Demand forecasting.
- Inventory optimization.
- Waste prediction.
- Business recommendations.

Not scoped, designed, or estimated yet. Revisit only after Phases 0–10 are
solid and in real use.

---

## How to use this file

Update the status markers as phases progress. When a phase's scope changes
materially (e.g. settlement granularity turns out to require redesign after
real sample data arrives), note the change here and cross-reference the
affected section of `ARCHITECTURE.md` rather than duplicating the detail.
