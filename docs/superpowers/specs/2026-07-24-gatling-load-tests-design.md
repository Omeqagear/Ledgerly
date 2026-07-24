# Phase 9c — Gatling Load/Performance Tests

**Date:** 2026-07-24
**Status:** Design approved

## Overview

Add Gatling load tests to verify Ledgerly's performance under realistic concurrent
load. Tests simulate CRUD-heavy user flows against the running docker-compose
stack, using JWT authentication. Data is ephemeral — each run starts from a
fresh DB.

## Architecture

| Component | Detail |
|---|---|
| **Tool** | Gatling 3.x via `gatling-maven-plugin` |
| **Simulation location** | `src/test/gatling/scala/com/ledgerly/` |
| **Run command** | `mvn gatling:test` |
| **Target** | `http://localhost:8080/api` (docker compose app) |
| **Auth** | Login as seeded admin, extract JWT, attach `Authorization: Bearer <token>` to every request |
| **Reports** | HTML in `target/gatling/` — per-simulation, with request timelines and percentiles |
| **Config** | `src/test/resources/gatling.conf` with base URL and ramp defaults |

## Simulations

### 1. `CustomerSimulation`

**Flow (per user):** Login → POST customer → GET /customers (paginated) → GET /customers/{id} → PUT /customers/{id} → DELETE /customers/{id}

- **Users:** 50
- **Ramp:** 30 seconds
- **Pacing:** 2s between actions
- **Assertions:** p99 < 1000ms, success rate > 99%

### 2. `InvoicePaymentSimulation`

**Flow (per user):** Login → POST customer → POST invoice → POST /invoices/{id}/issue → POST payment → GET /payments (verify)

- **Users:** 30
- **Ramp:** 30 seconds
- **Pacing:** 3s between actions
- **Assertions:** p99 < 2000ms, success rate > 99%

### 3. `ReportingSimulation`

**Flow (per user):** Login → GET /reports/summary → GET /reports/aging → GET /reports/customers/{customerId} → GET /reports/invoices/{invoiceId}/pdf → GET /reports/summary/excel

- **Users:** 20
- **Ramp:** 20 seconds
- **Pacing:** 5s between actions (heavier endpoints)
- **Assertions:** p99 < 5000ms (PDF/Excel are heavier), success rate > 99%
- **Setup:** Creates one customer + invoice per user before the read loop

### 4. `MixedWorkloadSimulation`

Combines all three flows in weighted proportions:
- 40% customer CRUD
- 35% invoice + payment
- 25% reporting reads

- **Users:** 50
- **Ramp:** 60 seconds

## Test Data Strategy

### Ephemeral, self-cleaning

1. Before run: `docker compose down --volumes && docker compose up --build -d`
2. Each simulation creates its own test data through the API
3. Customer/Invoice/Payment entities are created during the test and left in the compose volume
4. No explicit cleanup step — `docker compose down --volumes` between runs resets everything

### Seeded admin user

The `SeedUserRunner` creates `admin/ledgerly` on first boot. Simulations log in with
these credentials to obtain JWTs.

## Failure Thresholds

| Metric | Threshold |
|---|---|
| Overall error rate | < 5% |
| Write endpoint p95 | < 2000ms |
| Read endpoint p95 | < 1000ms |
| PDF/Excel p95 | < 5000ms |
| Login p95 | < 500ms |

These are starting baselines — tighten after observing real numbers.

## Project Changes

| File | Change |
|---|---|
| `pom.xml` | Add `gatling-maven-plugin` configuration |
| `src/test/resources/gatling.conf` | NEW — Gatling runtime config |
| `src/test/gatling/scala/com/ledgerly/CustomerSimulation.scala` | NEW |
| `src/test/gatling/scala/com/ledgerly/InvoicePaymentSimulation.scala` | NEW |
| `src/test/gatling/scala/com/ledgerly/ReportingSimulation.scala` | NEW |
| `src/test/gatling/scala/com/ledgerly/MixedWorkloadSimulation.scala` | NEW |
| `src/test/gatling/scala/com/ledgerly/LedgerlyProtocol.scala` | NEW — shared base URL, auth helper |

## Running the Tests

```bash
# Start app with fresh DB
docker compose down --volumes && docker compose up --build -d

# Wait for healthy
curl -s http://localhost:8080/api/actuator/health

# Run all simulations
mvn gatling:test

# Run a single simulation
mvn gatling:test -Dgatling.simulationClass=com.ledgerly.CustomerSimulation

# View reports
open target/gatling/*/index.html
```

## Descoped

- CI/CD integration (runs manually or in a separate CI job)
- k6 as an alternative tool (Gatling chosen)
- Baseline comparison across builds
- Grafana dashboard integration
- Tests against prod profile with Redis caching enabled
