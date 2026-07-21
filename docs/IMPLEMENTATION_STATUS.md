# Ledgerly — Implementation Status & Roadmap

This document describes what is currently implemented in the Ledgerly backend
and what remains from the original Modulith coding plan. It is intended as a
hand-off reference for the next development phase.

---

## Current implementation

Ledgerly is a **Spring Modulith** monolith with five business modules and a
shared `config` package. Each module has a public API package and an `internal/`
implementation package. Modules communicate through domain events where
decoupling is needed, and through public interfaces where synchronous validation
is required.

### Implemented modules

#### 1. Customer module (`com.ledgerly.customer`)

- Aggregate: `Customer`
- Public API: `CustomerAPI`, `CustomerLookup`, `CustomerService`
- REST endpoints: full CRUD under `/customers`
- Cross-module lookup: `CustomerLookup` exposes email/contact resolution for
  the notification module
- Validation:
  - Duplicate email rejected on create and update
  - `DataIntegrityViolationException` is translated to
    `DuplicateCustomerEmailException` to close the race window

#### 2. Invoice module (`com.ledgerly.invoice`)

- Aggregate: `Invoice` with lifecycle `DRAFT → ISSUED → PAID / OVERDUE`
- Public API: `InvoiceAPI` exposes read access and `markAsPaid(...)`
- REST endpoints: create, list, get, issue under `/invoices`
- Domain events: `InvoiceCreatedEvent`, `InvoicePaidEvent`
- Invoice numbers are generated as `INV-YYYY-NNNNNN` and seeded from the database
  on startup so the sequence is not reset on restart
- Scheduled job (`OverdueInvoiceMarker`) runs daily at midnight to transition
  past-due `ISSUED` invoices to `OVERDUE`

#### 3. Payment module (`com.ledgerly.payment`)

- Aggregate: `Payment` with status `PENDING → COMPLETED / FAILED`
- Public API: `PaymentAPI` for read access
- REST endpoints: create, list, get under `/payments`
- Gateway integration: `PaymentGatewayClient` interface with a stub
  implementation that always succeeds
- Validation: `PaymentService` loads the invoice through `InvoiceAPI`, verifies
  it is `ISSUED`/`OVERDUE`, and confirms the paid amount equals the invoice total
  before completing the charge
- Domain event: `PaymentProcessedEvent` (success + failure) is published for audit
  and failure notifications

#### 4. Notification module (`com.ledgerly.notification`)

- Event-driven only; no REST endpoints
- `NotificationEventListener` reacts to:
  - `InvoiceCreatedEvent` → "Invoice created" email
  - `InvoicePaidEvent` → "Payment received" email
  - `PaymentProcessedEvent` with `success=false` → "Payment failed" email
- `CustomerLookup` is used to resolve the recipient email without touching the
  customer entity directly
- `LoggingEmailSender` logs email metadata (recipient + subject) at INFO; body is
  logged at DEBUG only

#### 5. Reporting module (`com.ledgerly.reporting`)

- Read-only aggregations through `ReportRepository` (raw SQL via `JdbcTemplate`)
- REST endpoints:
  - `/reports/summary` — ledger-wide totals by status
  - `/reports/customers/{customerId}` — per-customer summary
- No compile-time dependencies on other business modules

### Shared infrastructure

- **Docker / Docker Compose** — multi-stage `Dockerfile`, compose stack with
  app, Postgres, and Prometheus
- **Flyway migrations** — `V1`–`V5` create `customers`, `invoices`, `payments`,
  `event_publication`, and `event_publication_archive`
- **Spring Modulith event persistence** — JPA-backed event publication registry
- **Observability** — Actuator + Prometheus scraping endpoint
- **Security** — profile-based split:
  - `dev` profile: open endpoints
  - `prod`/default: HTTP Basic with default user `ledgerly` / `ledgerly`
- **Global exception handling** — `RestExceptionHandler` maps domain and validation
  exceptions to proper HTTP status codes (400/404/409)
- **Tests** — `ModularityTests`, module-level `@ApplicationModuleTest` suites,
  and an end-to-end `@SpringBootTest` for the payment flow

### Architectural decisions

- **Payment → Invoice dependency, not the reverse.** The original plan had the
  invoice module listening to `PaymentProcessedEvent` to mark invoices paid. This
  made it impossible to validate the payment amount against the invoice total
  without introducing a cyclic module dependency. The current implementation has
  `payment` depend on `invoice` via `InvoiceAPI`, performs validation in the
  payment service, and synchronously marks the invoice paid on success.
  `InvoicePaidEvent` is still emitted by the invoice module and consumed by
  notification.
- **Reporting uses raw SQL.** This avoids loading all aggregates into memory and
  keeps the reporting module free of compile-time dependencies on other modules.

---

## Roadmap — remaining phases

### Phase 6 — Reporting enhancements (partially done)

**Status:** aggregations are implemented; PDF generation is not.

What remains:
- Generate PDF invoices/reports (e.g., with OpenPDF, iText, or JasperReports)
- Add date-range filters to `/reports/summary`
- Export customer statements as downloadable PDFs
- Add receivables aging report (0–30, 31–60, 61–90, 90+ days)

### Phase 7 — Security (partially done)

**Status:** HTTP Basic with a hard-coded in-memory user is in place.

What remains:
- Replace the dev-only in-memory user with JWT-based authentication
- Add role-based access control (e.g., `ADMIN`, `ACCOUNTANT`, `CUSTOMER`)
- Protect customer and invoice endpoints per role/ownership
- Refresh-token flow and logout handling
- Store users in the database (new `user` module or extend customer module)

### Phase 8 — Module verification & documentation generation

**Status:** architecture verification tests exist; generated documentation is not
persisted as build artifacts.

What remains:
- Run `Documenter.writeModulesAsPlantUml()` and `writeModuleCanvases()` in CI and
  commit/check the generated PlantUML / canvas diagrams
- Add a CI job that fails the build on `ApplicationModules.verify()` violations
- Publish module diagrams and dependency graphs to the README or a `docs/`
  folder automatically

### Phase 9 — Performance, caching, and optimization

**Status:** not started.

What remains:
- Add Redis for caching frequently read data (customer lookup, report summaries)
- Implement pagination and sorting on list endpoints
- Add database query analysis and optimize hot paths
- Add load/performance tests (e.g., Gatling or JMeter)
- Tune connection pool, JVM, and container resource limits

### Beyond the original plan

Items that surfaced during review and should be addressed before production:
- Replace the stub `PaymentGatewayClient` with a real PSP integration (Stripe,
  Adyen, etc.) and add idempotency keys
- Add idempotency keys for invoice/payment creation endpoints
- Add audit logging for payment and invoice state changes
- Implement event replay / event-sourcing readiness for the invoice aggregate
- Add resilience patterns: retry, circuit breaker, and outbox for event publication

---

## How to pick the next phase

1. **If the goal is a production-ready MVP:** start with Phase 7 (JWT/RBAC) and
   Phase 6 (PDF invoices), then the real payment gateway.
2. **If the goal is internal tooling:** Phase 6 (date-range reports, aging) and
   Phase 9 (Redis caching) give the most value.
3. **If the goal is hardening the Modulith architecture:** Phase 8 (CI-verified
   documentation + diagrams) and the "Beyond the original plan" audit/resilience
   items.
