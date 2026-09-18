# Ghost Kitchen — Architecture & Technical Design

**Subtitle:** Restaurant Revenue Intelligence & Reconciliation Platform
**Status:** Phase 1 — Architecture & Technical Design (no implementation yet)
**Last updated:** 2026-09-12

---

## 1. Problem Statement

Cloud kitchens and small restaurants sell through multiple channels (Swiggy,
Zomato, direct/WhatsApp orders). Delivery platforms issue settlement reports
containing gross sales and deductions (commission, advertising, cancellations,
penalties, taxes, adjustments). The restaurant then receives a lump-sum bank
payout. Owners currently have no reliable way to verify:

1. How much they should have received.
2. What deductions were applied and whether they're correct.
3. Whether the bank payout matches the expected payout.
4. Where and why a discrepancy occurred (commission error, ad fee, missed
   transaction, etc.).

Ghost Kitchen automates this reconciliation process.

**MVP scope:** reconciliation only. Revenue analytics and future intelligence
(demand forecasting, inventory optimization) are explicitly out of scope for
this phase — see Section 10.

---

## 2. Design Principles

- **No invented platform rules.** Commission structures, ad fee formats, and
  deduction categories are only hardcoded once real anonymized sample
  settlement reports are available from a real restaurant owner. Until then,
  only a generic/direct-order adapter is built against the common model.
- **Pluggable platform adapters.** Adding a new platform must never require
  changes to the reconciliation engine — only a new adapter implementation.
- **Deterministic, auditable reconciliation.** Discrepancy explanations are
  computed from actual numeric deltas against recorded deduction categories,
  not generated free text. Every result must be traceable back to source
  data.
- **Money is `NUMERIC`, never floating point.**
- **Portfolio-grade engineering bar.** Layered architecture, proper REST API
  design, DB migrations, auth, structured exception handling, and integration
  tests — not a CRUD toy.

---

## 3. Tech Stack

| Layer | Choice | Rationale |
|---|---|---|
| Backend | Java 21, Spring Boot 3 | Structured, enterprise-recognizable backend architecture |
| Build | Maven | Standard, predictable |
| Database | PostgreSQL 16 | `NUMERIC` for money, JSONB for raw-report snapshots, strong constraints |
| Migrations | Flyway | Versioned schema, required for a "production-ready" claim |
| ORM | Spring Data JPA + Hibernate | Standard with Spring Boot |
| Auth | Spring Security + JWT | Stateless REST auth, role-based access (OWNER / STAFF) |
| File parsing | Apache POI (Excel), OpenCSV (CSV) | Covers the common settlement-report formats |
| Frontend | React + TypeScript (Vite) | Fast dev loop, typed API contracts |
| API docs | springdoc-openapi (Swagger) | Self-documenting REST API |
| Testing | JUnit 5, Mockito, Testcontainers | Real Postgres in integration tests — reconciliation math must be tested against a real DB, not mocks |
| Containerization | Docker + docker-compose | One-command local run (app + Postgres) |

---

## 4. Component Architecture

```
                         +------------------------+
                         |    React Dashboard      |
                         +-----------+------------+
                                     | REST (JSON) + JWT
                         +-----------v------------+
                         |  API Layer (Controllers) |
                         +-----------+------------+
                                     |
        +---------------+-----------+----------+---------------+
        v               v           v          v               v
   Auth/Org         Ingestion   Reconciliation Analytics    Reporting
   Service          Service       Engine        Service      Service
        |               |           |
        |        +------v------+    |
        |        |  Platform   |    |
        |        |  Adapter    |    |
        |        |  Registry   |    |
        |        |  (Strategy) |    |
        |        +------+------+    |
        |        +------v------+    |
        |        |  Swiggy /   |    |
        |        |  Zomato /   |    |
        |        |  Direct     |    |
        |        |  Adapters   |    |
        |        +-------------+    |
        |                           |
        +-------------+-------------+
                       v
               +---------------+
               |  PostgreSQL   |
               +---------------+
```

Standard `Controller -> Service -> Repository` layering inside Spring Boot.
Ingestion, normalization, and reconciliation are separate service packages so
the reconciliation engine only ever depends on the common `PlatformTransaction`
model, never on raw file formats.

---

## 5. Domain Model

Multi-tenancy is supported from day one (cheap now, expensive to retrofit),
even though the initial deployment targets a single restaurant/kitchen.

### Core entities

- **`organization`** — the restaurant/cloud kitchen tenant.
  `id, name, created_at`

- **`app_user`** — login user.
  `id, organization_id, email, password_hash, role ENUM(OWNER, STAFF), created_at`

- **`platform`** — lookup table for sales channels (Swiggy, Zomato, Direct,
  ...). A row, not a hardcoded enum, so new platforms don't require schema
  changes.
  `id, code, display_name`

- **`settlement_report`** — one uploaded platform settlement file.
  `id, organization_id, platform_id, file_name, file_hash, uploaded_at,
  period_start, period_end, status ENUM(PENDING, PARSED, FAILED),
  raw_payload JSONB, error_detail`

- **`platform_transaction`** — the **common normalized model** every adapter
  must produce; one row per order/line-item from a settlement report.
  `id, settlement_report_id, platform_order_id, order_date,
  gross_amount NUMERIC(12,2), commission NUMERIC(12,2),
  advertising_fee NUMERIC(12,2), cancellation_penalty NUMERIC(12,2),
  tax_adjustment NUMERIC(12,2), other_deduction NUMERIC(12,2),
  net_expected_payout NUMERIC(12,2), raw_line_ref JSONB`

- **`bank_statement`** — one uploaded bank statement file.
  `id, organization_id, file_name, file_hash, uploaded_at`

- **`bank_transaction`** — normalized bank line item.
  `id, bank_statement_id, txn_date, amount NUMERIC(12,2), narration,
  reference_no, matched BOOLEAN`

- **`reconciliation_run`** — one execution of the engine.
  `id, organization_id, platform_id, period_start, period_end,
  executed_at, status`

- **`reconciliation_result`** — output per matched unit.
  `id, reconciliation_run_id, platform_transaction_id (nullable),
  bank_transaction_id (nullable), expected_amount, actual_amount,
  difference, status ENUM(MATCHED, UNDERPAID, OVERPAID, MISSING, UNEXPLAINED),
  explanation`

  Designed to support **many-to-one** matching (multiple
  `platform_transaction` batch rows reconciling against a single lump-sum
  `bank_transaction`), since real-world platform payouts commonly batch
  multiple orders into one settlement. This must be confirmed against real
  sample data — see Section 9.

### File uploads
Uploaded files are hashed (`file_hash`) per organization+platform+period to
reject/flag duplicate re-uploads and prevent double-counting.

---

## 6. Platform Adapter Abstraction (core extensibility point)

```java
public interface SettlementReportAdapter {
    Platform supportedPlatform();
    List<PlatformTransaction> parse(RawReportFile file) throws ReportParseException;
}
```

- Each platform implements this interface (`SwiggyAdapter`, `ZomatoAdapter`,
  `DirectOrderAdapter`, ...).
- A `PlatformAdapterRegistry` (Spring-managed `Map<Platform, SettlementReportAdapter>`)
  dispatches parsing by platform type.
- **Nothing downstream of `parse()`** — reconciliation engine, matching,
  discrepancy detection — is aware of platform-specific fields. They operate
  only on `PlatformTransaction`.
- Adding a new platform = one new adapter class + one registry entry. Zero
  changes to the reconciliation engine.
- Initial build only implements `DirectOrderAdapter` (format we control) and a
  generic CSV adapter, to validate the interface. Swiggy/Zomato adapters are
  built only once real sample settlement reports are available.

---

## 7. Reconciliation Algorithm

1. For a given organization + platform + period, sum
   `platform_transaction.net_expected_payout` → **expected payout**.
2. Search `bank_transaction` for candidate matches: same organization, within
   a configurable date window (settlements commonly lag the order date), and
   amount within a configurable tolerance (e.g. ₹1 rounding).
3. Classify each match:
   - **MATCHED** — bank amount equals expected amount within tolerance.
   - **UNDERPAID** — bank amount is less than expected; difference is
     broken down against recorded deduction categories where possible.
   - **OVERPAID** — bank amount exceeds expected.
   - **MISSING** — expected payout exists with no corresponding bank
     transaction found in the window.
   - **UNEXPLAINED** — a bank transaction exists with no traceable
     settlement report backing it.
4. Discrepancy explanations are computed deterministically (diff vs. sum of
   each deduction category) — no free-text generation, fully auditable.
5. Matching starts as deterministic (date window + amount tolerance), fully
   documented and configurable. Fuzzy/heuristic matching is only considered
   later, and only once real data shows the deterministic approach is
   insufficient — never invented ahead of evidence.

---

## 8. API Surface (initial)

```
POST   /api/auth/register
POST   /api/auth/login

POST   /api/settlement-reports/upload      (multipart; platform_id, period)
GET    /api/settlement-reports/{id}
GET    /api/platform-transactions?reportId=

POST   /api/bank-statements/upload         (multipart)
GET    /api/bank-statements/{id}

POST   /api/reconciliation/runs            (trigger a run for org+platform+period)
GET    /api/reconciliation/runs/{id}
GET    /api/reconciliation/results?runId=&status=

GET    /api/analytics/summary?period=
GET    /api/analytics/by-platform?period=
```

All endpoints except `/api/auth/*` require a JWT. `organization_id` scoping is
always derived from the authenticated user server-side — never trusted from
client-supplied input.

---

## 9. Ingestion & Error Handling

- Upload → raw file stored + `settlement_report` row created as `PENDING` →
  matching adapter parses the file → on success, `platform_transaction` rows
  inserted and status set to `PARSED`; on failure, status `FAILED` with a
  structured `error_detail` (bad format, missing required column, unreadable
  file) — never a raw stack trace surfaced to the client.
- A global `@ControllerAdvice` exception handler maps parse errors, validation
  errors, and auth failures to consistent JSON error responses.
- Idempotency via `file_hash`: re-uploading an identical file for the same
  organization+platform+period is rejected or flagged to prevent
  double-counting.

---

## 10. Explicitly Out of Scope (MVP)

- Revenue analytics beyond basic summary/by-platform endpoints.
- Demand forecasting, inventory optimization, waste prediction (see long-term
  vision below).
- Any platform-specific deduction rule not yet confirmed by real sample data.
- Automated "fix" or "dispute" workflows with the platforms — this is a
  read-only reconciliation and reporting tool.

### Long-term vision (not built now)

```
GHOST KITCHEN
├── Revenue Reconciliation
│   ├── Swiggy
│   ├── Zomato
│   ├── Direct/WhatsApp orders
│   └── Bank settlements
│
├── Revenue Analytics
│   ├── Gross revenue
│   ├── Net revenue
│   ├── Commission
│   ├── Advertising
│   └── Profit-related insights
│
└── Future Intelligence
    ├── Demand forecasting
    ├── Inventory optimization
    ├── Waste prediction
    └── Business recommendations
```

---

## 11. Open Items / Assumptions Pending Real Data

1. **Multi-tenancy**: assumed supported from day one in the schema (see
   Section 5), even though initial usage is single-restaurant.
2. **Settlement granularity**: schema is designed to support many-to-one
   matching (multiple platform transaction batches → one bank payout), since
   this is common in real Swiggy/Zomato settlements. **This must be verified
   against real sample reports** before the matching logic in Section 7 is
   finalized.
3. **Platform-specific deduction fields/column mapping**: intentionally not
   defined yet for Swiggy/Zomato. Will be derived directly from real
   anonymized sample settlement reports when available, per the project's
   "no invented platform rules" principle.

---

## 12. Next Steps

- Phase 2: Finalize DB schema as Flyway migration scripts + JPA entities.
- Phase 3: Scaffold Spring Boot project structure, auth, and the
  `SettlementReportAdapter` interface + `DirectOrderAdapter`.
- Phase 4: Build the reconciliation engine against synthetic test data
  matching the example in the problem statement.
- Phase 5: React dashboard skeleton (upload flow, results table, discrepancy
  detail view).
- Real Swiggy/Zomato adapters, deduction rules, and settlement granularity
  are finalized only once real anonymized sample reports are obtained.
