# Agent Notes for Ledgerly

High-signal context for OpenCode sessions. If a fact is obvious from the
README or file names, it is not here.

## Build & test environment

- **Host has Java 11 and no Maven.** Do not assume `mvn` or JDK 21 are available.
  Always build and test through Docker:

  ```bash
  # Build
  docker run --rm -v ${PWD}:/app -w /app maven:3.9.9-eclipse-temurin-21 \
    mvn -B -ntp clean package

  # Test
  docker run --rm -v ${PWD}:/app -w /app maven:3.9.9-eclipse-temurin-21 \
    mvn -B -ntp test
  ```

- Tests run against **H2 in-memory** (`src/test/resources/application.yml`).
  No Postgres container is needed for `mvn test`.

- The Maven APT processor is disabled (`<proc>none</proc>`). Modulith
  architecture verification happens at runtime via `ModularityTests`.

## Running the application

- Use Docker Compose:

  ```bash
  docker compose up --build -d
  ```

- Compose activates the **prod** profile. Business endpoints require HTTP Basic
  auth:
  - Username: `ledgerly`
  - Password: `ledgerly`
  - Actuator `/health` is public; everything else under `/api` is authenticated.

- The **dev** profile keeps endpoints open for local development. Tests do not
  exercise HTTP, so the security filter chain does not affect the existing test
  suite.

- Application context-path is `/api`. All endpoints are under `http://localhost:8080/api`.

## Database / migrations

- **Flyway owns the schema.** Do not mount migration scripts into Postgres's
  `/docker-entrypoint-initdb.d` (this was a previous bug that caused a
  restart loop). When changing migrations, run `docker compose down --volumes`
  before `up --build -d` so the `postgres-data` volume is recreated.

- Spring Modulith JPA event publication requires two tables:
  `event_publication` and `event_publication_archive`. They are created by
  Flyway migrations `V4` and `V5`, not by Hibernate or by the
  `jdbc.schema-initialization` property.

## Module structure

- Spring Modulith modules are **direct sub-packages of `com.ledgerly`**:
  `customer`, `invoice`, `payment`, `notification`, `reporting`, `config`.
- Each module exposes a public API at its root package and hides
  implementation details under `internal/`.
- **Module dependency direction:** `payment` depends on `invoice` via
  `InvoiceAPI`. The reverse is not allowed — it would create a cyclic
  dependency because `invoice` emits events consumed by `notification`.
- `reporting` reads via raw SQL (`JdbcTemplate`) and has no compile-time
  dependencies on other modules.

## Domain behavior gotchas

- Invoices are **paid through the payment module**, not via an invoice endpoint.
  `POST /api/payments` validates the invoice state (`ISSUED`/`OVERDUE`) and
  amount, then calls `InvoiceAPI.markAsPaid(...)` synchronously.
- `PaymentGatewayClient.DefaultPaymentGatewayClient` is a stub that always
  succeeds. Replace it with a real PSP integration in production.
- Invoice numbers (`INV-YYYY-NNNNNN`) are seeded from the database on startup
  (`InvoiceNumberGenerator`), so the sequence survives restarts.
- `OverdueInvoiceMarker` runs daily at midnight (`0 0 0 * * ?`) and marks
  past-due `ISSUED` invoices as `OVERDUE`.
- `LoggingEmailSender` logs recipient and subject at INFO; body is DEBUG only.

## Git / repository hygiene

- `target/` is build output and must not be committed. `.gitignore` blocks it.
  If `target/` is still tracked in the index, run:

  ```bash
  git rm -r --cached target/
  ```

## Verification order

1. Edit code.
2. Run `mvn test` in Docker.
3. If migrations changed, `docker compose down --volumes && docker compose up --build -d`.
4. Smoke-test with `curl -u ledgerly:ledgerly http://localhost:8080/api/...`.
