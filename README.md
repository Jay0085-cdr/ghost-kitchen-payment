# Ghost Kitchen

Restaurant revenue reconciliation. A cloud kitchen sells through Swiggy,
Zomato, and direct/WhatsApp orders. Each platform sends a settlement report
(gross sales minus commission/ad fees/etc.) and then pays out a lump sum to
the bank. Ghost Kitchen checks whether the bank payout actually matches what
the platform said it owed, and flags exactly where it didn't.

**Status: MVP complete and verified running locally** — backend, frontend,
PostgreSQL, file uploads, and reconciliation have all been exercised
end-to-end together, including through the actual UI, not just the API.
See `ROADMAP.md` for phase-by-phase build history and what was verified at
each step. `ARCHITECTURE.md` has the full design rationale.

---

## Features

- **Auth** — register creates a new organization with you as its `OWNER`;
  login issues a JWT. Every request after that is scoped to your
  organization from the token — never from anything the client sends.
- **Upload settlement reports** (CSV) — parsed into per-order line items,
  duplicate uploads rejected by file hash, bad files fail cleanly with a
  reason instead of crashing.
- **Upload bank statements** (CSV) — same idea, turned into bank line items.
- **Run reconciliation** — for a platform + date range, matches each order
  to the closest bank payout (date window + amount tolerance, both
  configurable) and classifies it `MATCHED` / `UNDERPAID` / `OVERPAID` /
  `MISSING` / `UNEXPLAINED`. Plain rule-based Java — no AI/LLM anywhere in
  the matching or the numbers.
- **Dashboard** — organization summary, status counts across all runs,
  recent run history.
- **Discrepancy breakdown** — click into any result to see why it was
  flagged.

---

## Architecture

```
React (Vite, TS)  ──HTTP + JWT──▶  Spring Boot  ──JDBC──▶  PostgreSQL 16
                                   │
                                   ├─ auth/            register, login, JWT
                                   ├─ organization/     org lookup, tenant scoping
                                   ├─ platform/          Swiggy/Zomato/Direct lookup
                                   ├─ ingestion/         CSV upload → normalized rows
                                   │    └─ adapter/      per-platform CSV parsing (pluggable)
                                   ├─ reconciliation/    matching engine + REST layer
                                   ├─ security/          stateless JWT filter
                                   └─ exception/         one JSON error shape for everything
```

- **Multi-tenant from day one**: every business table carries
  `organization_id`; the JWT — not the client — is the only source of truth
  for which organization a request belongs to.
- **Money is `NUMERIC(12,2)` everywhere**, never floating point.
- **Adapters are pluggable**: a new platform is one new class implementing
  `SettlementReportAdapter` — the reconciliation engine never knows which
  platform a transaction came from.
- **The reconciliation engine takes plain data in, returns plain data out**
  — no Spring, no database, fully unit-testable with dummy objects (see
  `ReconciliationEngineTest`).

Full schema, relationships, and the design decisions behind them are in
`ARCHITECTURE.md`.

---

## Tech stack

| Layer | Choice |
|---|---|
| Backend | Java 21, Spring Boot 3.3, Spring Security (JWT), Spring Data JPA |
| Database | PostgreSQL 16, Flyway migrations |
| Frontend | React 19 + TypeScript, Vite, React Router |
| Auth | Stateless JWT (HS256), BCrypt password hashing |

---

## Setup

### Prerequisites

- Java 21 (JDK, not just JRE)
- Maven 3.9+
- PostgreSQL 16
- Node.js 20+ / npm
- Git

Don't have these installed? Portable (no-admin-install) zip builds work
fine: [Adoptium](https://adoptium.net) for the JDK,
[Apache Maven](https://maven.apache.org/download.cgi) binary zip,
[EDB's Postgres "Binaries" zip](https://www.enterprisedb.com/download-postgresql-binaries)
(not the interactive installer), and a
[PortableGit release](https://github.com/git-for-windows/git/releases) for
Git. Extract each and add `<tool>/bin` to your PATH.

### 1. Database

```powershell
# Initialize a data directory (once)
initdb -D <path-to-data-dir> -U postgres -A trust --encoding=UTF8

# Start Postgres (leave this running in its own terminal)
pg_ctl -D <path-to-data-dir> -l pg.log start

# Create the database
createdb -U postgres ghost_kitchen
```

`-A trust` skips password checks — fine for a local throwaway dev database,
not for anything else.

### 2. Backend

```powershell
cd ghost-kitchen-payment

# JWT_SECRET must be at least 32 bytes or the app refuses to start
$env:JWT_SECRET = -join ((48..57)+(65..90)+(97..122) | Get-Random -Count 48 | ForEach-Object {[char]$_})

mvn spring-boot:run
```

On success you'll see Flyway apply 12 migrations, then Tomcat start on port
8080. If your Postgres isn't on the default `localhost:5432` /
`postgres`/`postgres`, override it with env vars rather than editing
`application.yml` (its defaults are the intended production baseline):

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5433/ghost_kitchen"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "postgres"
```

### 3. Frontend

```powershell
cd ghost-kitchen-payment/frontend
npm install
npm run dev
```

Opens on `http://localhost:5173` (or the next free port — 5174, 5175, etc.
— if that one's taken). Talks to the backend at `http://localhost:8080` by
default; override with a `.env.local` containing `VITE_API_BASE_URL=...`.

### 4. Try it

1. Open the frontend URL, register a kitchen.
2. Go to **Uploads**, upload a settlement report CSV (see format below) and
   a bank statement CSV.
3. Go to **Reconciliation**, select the platform and a date range covering
   your uploads, click **Run reconciliation**.
4. Click into the run to see per-order results and discrepancies.

---

## Sample CSV formats

**Settlement report — Direct platform** (`platform` dropdown = Direct):
```csv
order_id,order_date,gross_amount,other_deduction
ORD-1001,2026-09-01,1000.00,20.00
ORD-1002,2026-09-02,500.00,0
```

**Settlement report — any other platform** (generic fallback adapter,
flexible column names — `order_id`/`platform_order_id`, `gross_amount`/
`gross`, `commission`, `advertising_fee`, `net_expected_payout`/`payout` are
all recognized):
```csv
order_id,order_date,gross_amount,commission,net_expected_payout
ORD-2001,2026-09-01,1000.00,150.00,850.00
```

**Bank statement:**
```csv
txn_date,amount,narration,reference_no
2026-09-03,980.00,SWIGGY SETTLEMENT,REF001
2026-09-04,500.00,DIRECT PAYOUT,REF002
```

---

## API reference

All endpoints except `/api/auth/*` require `Authorization: Bearer <token>`.
Full request/response shapes are in the DTO classes next to each
controller — this is the map, not the full spec.

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/auth/register` | Create organization + owner account, returns JWT |
| POST | `/api/auth/login` | Returns JWT |
| GET | `/api/organizations/{id}` | Org details (only your own — others 403) |
| GET | `/api/platforms` | List Swiggy/Zomato/Direct for dropdowns |
| POST | `/api/settlement-reports/upload` | Multipart: `file`, `platformId`, `periodStart`, `periodEnd` |
| GET | `/api/settlement-reports` | List your uploads |
| GET | `/api/settlement-reports/{id}` | One upload's detail |
| GET | `/api/platform-transactions?reportId=` | Line items from one report |
| POST | `/api/bank-statements/upload` | Multipart: `file` |
| GET | `/api/bank-statements` | List your uploads |
| GET | `/api/bank-statements/{id}` | One upload's detail + its transactions |
| POST | `/api/reconciliation/runs` | Body: `{ platformId, periodStart, periodEnd }` — triggers matching |
| GET | `/api/reconciliation/runs` | List your runs, newest first |
| GET | `/api/reconciliation/runs/{id}` | One run's status counts |
| GET | `/api/reconciliation/results?runId=&status=` | Per-order verdicts, optional status filter |
| GET | `/api/reconciliation/results/{id}/discrepancies` | Why one result was flagged |

Errors always come back as:
```json
{ "timestamp": "...", "status": 404, "error": "Not Found", "message": "...", "path": "/api/..." }
```

---

## Testing

```powershell
mvn test        # backend — 10 reconciliation engine tests, no DB needed
npx tsc -b       # frontend type-check
npm run build    # frontend production build
```

There's no automated integration test suite yet (would need Testcontainers +
a real Postgres) — the full flow has been verified manually end-to-end, but
that's not a substitute for a regression-proof automated suite. Worth
closing next.

---

## Deployment

`docker-compose.yml`, `Dockerfile` (backend), and `frontend/Dockerfile` are
in this repo and correct by inspection, but Docker itself hasn't been run
in this environment — verify locally before trusting it in production.

### Local, via Docker

```powershell
# Create a .env file in the repo root:
# JWT_SECRET=<32+ random characters>
# DB_PASSWORD=<your choice>

docker compose up --build
```
Backend on `:8080`, frontend on `:5173`, Postgres on `:5432` (named volume,
so data survives restarts).

### Cloud

- **Backend**: any host that runs a Docker image + gives you a Postgres
  instance (Render, Railway, Fly.io all fit). Point it at the `Dockerfile`
  in the repo root, set `DB_URL`/`DB_USERNAME`/`DB_PASSWORD`/`JWT_SECRET` as
  environment variables — never hardcode them.
- **Frontend**: any static host that can run `npm run build` (Vercel,
  Netlify, or the `frontend/Dockerfile` + nginx image). Set
  `VITE_API_BASE_URL` to wherever the backend ends up.

---

## Known gaps (honest list)

- No automated integration test suite (Testcontainers) — only unit tests
  plus one-time manual end-to-end verification.
- Uploaded settlement report / bank statement files aren't stored anywhere
  retrievable — only their hash and the parsed rows survive.
- Reconciliation matches one order to one bank payout (1:1). Real
  settlements may batch many orders into a single payout — unconfirmed
  without a real Swiggy/Zomato sample report, so deliberately left simple.
- No Swiggy/Zomato-specific adapters yet — uploads for those platforms fall
  back to a generic, flexible-column CSV parser until real sample reports
  are available to build against (see `ARCHITECTURE.md` §2, "no invented
  platform rules").
- Docker configs are unverified (no Docker available to test with locally).

---

## Folder map

```
ARCHITECTURE.md, ROADMAP.md      design doc / phase history
pom.xml, Dockerfile               backend build + container
docker-compose.yml                postgres + backend + frontend, wired together

src/main/resources/
  application.yml                 DB / JWT / upload config (env-var driven)
  db/migration/                   Flyway SQL, V1–V12

src/main/java/com/ghostkitchen/
  entity/, repository/            one class + repo per DB table
  security/, config/               stateless JWT auth
  auth/, organization/, platform/  register/login, org + platform lookups
  ingestion/                       CSV upload, parsing, adapters
  reconciliation/                  matching engine + REST layer
  exception/                       shared error handling

src/test/java/.../reconciliation/  engine unit tests

frontend/
  src/api/client.ts                 fetch wrapper (JWT header, error handling)
  src/auth/                         login state
  src/pages/                        Dashboard, Uploads, Reconciliation, Run detail
  Dockerfile, nginx.conf            static-serve container
```
