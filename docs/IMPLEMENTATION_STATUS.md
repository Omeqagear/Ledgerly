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
- Document generation via OpenPDF (PDF) and Apache POI (Excel)
- Depends on `invoice` and `customer` modules via `InvoiceAPI` / `CustomerService`
- Date-range filtering on summary reports (applies only to invoice-by-status)
- Aging reports with 5 buckets (Current, 1–30, 31–60, 61–90, 90+ days)
- REST endpoints:
  - `/reports/summary?from=&to=` — date-filtered JSON summary
  - `/reports/aging` — ledger-wide aging report
  - `/reports/customers/{customerId}` — per-customer summary
  - `/reports/customers/{customerId}/aging` — per-customer aging
  - `/reports/invoices/{id}/pdf` — invoice PDF download (OpenPDF)
  - `/reports/customers/{id}/statement.pdf` — customer statement PDF with transaction history
  - `/reports/summary/excel` — Excel summary export (Apache POI)
  - `/reports/aging/excel` — Excel aging export
  - `/reports/customers/{id}/excel` — Excel customer summary export
- Module-level exception handler (`ReportingExceptionHandler`)

#### 6. User module (`com.ledgerly.user`)

- Aggregate: `User` (authentication account — distinct from a billing `Customer`)
  with fields `id`, `username`, `passwordHash`, `role`, `createdAt`
- Public API: `UserService` (CRUD + password encoding), `UserController`
- REST endpoints under `/users` (create, list, get, change password, delete)
- Passwords are BCrypt-hashed; raw passwords are never stored
- Duplicate username rejected on create and update
  (`DataIntegrityViolationException` → `DuplicateUserException`)
- `SeedUserRunner` creates a configurable admin user on first boot
  (`LEDGERLY_SEED_ADMIN_USERNAME` / `LEDGERLY_SEED_ADMIN_PASSWORD`)

#### 7. Auth module (`com.ledgerly.auth`)

- `POST /auth/login` validates credentials against the `user` store and returns a
  signed JWT (`{token, expiresIn, username, role}`)
- `JwtService` encodes/decodes HS256 JWTs via Spring Security OAuth2 Resource
  Server + Nimbus
- Token claims: `sub` = username, `role` = `ADMIN`/`USER`, `iat`, `exp` (30 min)
- `internal/ApplicationUserDetailsService` loads `User` via `UserService` and maps
  the role to a `ROLE_<role>` granted authority
- `AuthConfig` exposes `SecretKey`, `JwtService`, and `JwtDecoder` beans

### Shared infrastructure

- **Docker / Docker Compose** — multi-stage `Dockerfile`, compose stack with
  app, Postgres, and Prometheus
- **Flyway migrations** — `V1`–`V6` create `customers`, `invoices`, `payments`,
  `event_publication`, `event_publication_archive`, and `users`
- **Spring Modulith event persistence** — JPA-backed event publication registry
- **Observability** — Actuator + Prometheus scraping endpoint
- **Security** — profile-based split:
  - `dev` profile: open endpoints
  - `prod`/default: stateless JWT resource server (HS256). `/auth/login` and the
    actuator health/info/prometheus/modulith endpoints are public; `/users/**` is
    `ADMIN`-only; everything else under `/api` requires a valid JWT
- **Global exception handling** — `RestExceptionHandler` maps domain and validation
  exceptions to proper HTTP status codes (400/404/409)
- **Tests** — `ModularityTests` (architecture verification), `ModulithDocumentationTests`
  (diagram generation, gated), module-level `@ApplicationModuleTest` suites, and
  end-to-end `@SpringBootTest` flows for payment and auth

### Architectural decisions

- **Payment → Invoice dependency, not the reverse.** The original plan had the
  invoice module listening to `PaymentProcessedEvent` to mark invoices paid. This
  made it impossible to validate the payment amount against the invoice total
  without introducing a cyclic module dependency. The current implementation has
  `payment` depend on `invoice` via `InvoiceAPI`, performs validation in the
  payment service, and synchronously marks the invoice paid on success.
  `InvoicePaidEvent` is still emitted by the invoice module and consumed by
  notification.
- **Reporting uses raw SQL + document generation.** Reports are powered by raw
  SQL via `JdbcTemplate` (no ORM overhead) and document generation via OpenPDF
  and Apache POI. Reports share dependency direction (reporting → invoice +
  customer) via `InvoiceAPI` and `CustomerService`.
- **Auth is a separate module from user.** `auth` depends on `user` (to load
  credentials) and exposes the login endpoint + `JwtService`. `config` depends on
  `auth` for JWT resource-server wiring. `user` has no dependency on `auth` or
  `config`, so no cycle is introduced.
- **Phase 7 scope was deliberately narrowed to access tokens.** The original
  roadmap listed refresh tokens, logout, the `CUSTOMER`/`ACCOUNTANT` roles, and
  per-ownership endpoint protection. The agreed design spec shipped only
  short-lived access tokens (30 min) and `ADMIN`/`USER` roles; the rest is
  deferred to "Beyond the original plan".

---

## Roadmap — remaining phases

### Phase 6 — Reporting enhancements (COMPLETED)

**Status:** All planned features implemented. PDF generation (OpenPDF), Excel
export (Apache POI), date-range filtering, aging reports (ledger-wide +
per-customer), and customer statement PDFs are all live.

### Phase 7 — Security (COMPLETED)

**Status:** HTTP Basic with the hard-coded in-memory user has been replaced by
stateless JWT authentication backed by a database user store, with role-based
access control.

What was delivered:
- New `user` module (`User` entity, `UserService`, `UserController`, Flyway `V6`)
  with BCrypt-hashed passwords and a seeded admin user
- New `auth` module (`POST /auth/login`, `JwtService`,
  `ApplicationUserDetailsService`) issuing HS256 JWTs (30-min expiry)
- `SecurityConfig` switched to a Spring Security OAuth2 resource server;
  `/auth/login` + actuator endpoints public, `/users/**` `ADMIN`-only, the rest
  authenticated
- Tests: `UserServiceTest`, `JwtServiceTest`, `AuthModuleIntegrationTest`,
  `SecurityIntegrationTest`

What was descoped (deferred to "Beyond the original plan"):
- Refresh-token flow and logout / token blocklist
- `CUSTOMER` / `ACCOUNTANT` roles and per-ownership endpoint protection

### Phase 8 — Module verification & documentation generation (COMPLETED)

**Status:** architecture verification is enforced in CI, and generated PlantUML
diagrams / module canvases are committed as build artifacts.

What was delivered:
- `ModulithDocumentationTests` runs `Documenter.writeModulesAsPlantUml()`,
  `writeIndividualModulesAsPlantUml()`, `writeModuleCanvases()`, and
  `writeAggregatingDocument()` into the committed `docs/modulith/` folder. It is
  gated behind `-Dgen-modulith-docs=true` so the normal test suite does not wipe
  committed artifacts.
- GitHub Actions workflow (`.github/workflows/ci.yml`):
  - `verify` job runs `mvn test`, which fails the build on any
    `ApplicationModules.verify()` violation
  - `docs` job regenerates the diagrams and commits them back on push to the
    default branch (`[skip ci]` to avoid loops)
- README links to `docs/modulith/` and documents how to regenerate the diagrams
  locally

### Phase 9 — Performance, caching, and optimization

**Status:** split into sub-phases. Phase 9a (pagination & sorting) is complete;
9b–9e are not started.

Phase 9 was broken into independent sub-plans (each is self-contained and
testable on its own):

#### Phase 9a — Pagination & sorting (COMPLETED)

**Status:** all planned features implemented. 101 tests pass (0 failures).

- Design spec: `docs/superpowers/specs/2026-07-24-pagination-sorting-design.md`
- Implementation plan: `docs/superpowers/plans/2026-07-24-pagination-sorting.md`

What was delivered:
- `spring.data.web.pageable` config (default page size 20, max 100) in both
  main and test `application.yml`
- Customer, invoice, payment, and user list endpoints now return Spring Data
  `Page<T>` instead of bare arrays, accepting `?page`, `?size`, and
  `?sort=field,dir` (repeatable) query params
- Existing `List<T>` service methods kept where internal callers exist
  (invoice's `OverdueInvoiceMarker`); payment's `List` variants replaced since
  they had no internal callers
- `PaymentAPI` trimmed to `findById` only (filtered methods were unused by
  other modules)
- `PaymentController` no-filter branch fixed: returns `findAll(pageable)`
  instead of `List.of()`
- `Page.map(UserResponse::from)` used in the user controller to preserve the
  DTO mapping
- 5 new test classes: per-module pagination integration tests + an HTTP-level
  MockMvc test verifying query-param binding, sort application, and size
  clamping

#### Phase 9b — Redis caching (COMPLETED)

**Status:** all planned features implemented.

What was delivered:
- `spring-boot-starter-data-redis` dependency for Redis connectivity and
  `RedisCacheManager` auto-configuration
- `CacheConfig` (`@EnableCaching`) with per-region TTLs: `customers` cache
  (no TTL, eviction-based) and `reports` cache (5-min TTL)
- `CustomerLookupImpl` methods (`findEmailById`, `findInfoById`, `exists`)
  annotated with `@Cacheable`
- `CustomerService` mutations (`createCustomer`, `updateCustomer`,
  `deleteCustomer`) annotated with `@CacheEvict`
- `ReportService` summary methods (`overallSummary`, `customerSummary`,
  `agingReport`, `agingReportForCustomer`) annotated with `@Cacheable`
- `application.yml`: default `spring.cache.type=simple` (in-memory) for
  dev; prod overrides to `redis` via docker compose environment
- `docker-compose.yml`: Redis 7 Alpine service, app service wired to it
- Test `application.yml`: `spring.cache.type=none` to keep existing
  module tests isolated
- `CachingIntegrationTest`: 6 tests using Testcontainers Redis verifying
  cache population, eviction, and report caching (tagged `redis`, run
  with `mvn test -Dgroups=redis` when Docker is available)
- `testcontainers-redis` 2.2.2 test dependency

#### Phase 9c — Load/performance tests (NOT STARTED)

- Add Gatling or k6 load tests against the running app
- Best done after pagination + caching land

#### Phase 9d — Query optimization (NOT STARTED)

- Add DB indexes, EXPLAIN analysis, optimize `ReportRepository` hot SQL
- Most valuable once load tests identify the hot paths

#### Phase 9e — Resource tuning (NOT STARTED)

- Tune HikariCP pool, JVM heap, container memory/CPU limits
- Depends on load test results to be meaningful

### Beyond the original plan

Items that surfaced during review and should be addressed before production:
- Refresh-token rotation and a logout / JWT blocklist flow (descoped from Phase 7)
- Per-ownership endpoint protection and finer-grained roles
  (`CUSTOMER` / `ACCOUNTANT`) (descoped from Phase 7)
- Replace the stub `PaymentGatewayClient` with a real PSP integration (Stripe,
  Adyen, etc.) and add idempotency keys
- Add idempotency keys for invoice/payment creation endpoints
- Add audit logging for payment and invoice state changes
- Implement event replay / event-sourcing readiness for the invoice aggregate
- Add resilience patterns: retry, circuit breaker, and outbox for event publication

---

## How to pick the next phase

Phases 6, 7, 8, and 9a are complete. Phase 9b–9e remain. The remaining
roadmap work is:

1. **If the goal is a production-ready MVP:** the next priority is the real
   payment gateway integration ("Beyond the original plan") and then Phase 9b
   (Redis caching) and Phase 9c (load testing).
2. **If the goal is internal tooling:** Phase 9b (Redis caching) gives the
   most value now that pagination is in place.
3. **If the goal is hardening the auth story:** the descoped Phase 7 items
   (refresh tokens, logout, per-ownership protection, finer-grained roles) come
   next.
