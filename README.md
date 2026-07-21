# Ledgerly

A modular invoice-management backend built as a **Spring Modulith** monolith.
Ledgerly keeps customer master data, invoices, payments, notifications, and
reporting in cleanly separated compile-time modules that communicate through
domain events instead of direct service calls.

> **Status:** functional scaffold. Authentication, overdue scheduling, and real
> payment-gateway integration are deferred to later roadmap phases.

---

## Tech stack

| Layer | Technology |
|-------|-----------|
| Java | OpenJDK 21 |
| Build | Maven 3.9.x |
| Framework | Spring Boot 3.2.5 |
| Modularity | Spring Modulith 1.3.0 |
| Database | PostgreSQL 16 |
| Migrations | Flyway 9 |
| Events | Spring Modulith Events (JPA persistence) |
| Observability | Spring Boot Actuator + Prometheus |
| Containerization | Docker + Docker Compose |

---

## Architecture

Spring Modulith detects modules as direct sub-packages of `com.ledgerly`.
Each module exposes a public API at its root package and hides implementation
details under `internal/`.

```
com.ledgerly
├── customer          # customer master data + lookup contracts
├── invoice           # invoice lifecycle + InvoiceCreatedEvent / InvoicePaidEvent
├── payment           # payment processing + PaymentProcessedEvent
├── notification      # event-driven email dispatch
├── reporting         # read-only aggregations across all modules
└── config            # security, observability
```

### Module dependencies

```
invoice  ──provides──▶ payment      (InvoiceAPI: validate + mark paid)

notification ──consumes──▶ customer  (CustomerLookup)
             ──consumes──▶ invoice   (InvoiceCreatedEvent, InvoicePaidEvent)
             ──consumes──▶ payment   (PaymentProcessedEvent)

reporting  ──reads──▶  customer, invoice, payment  (raw SQL, no compile-time deps)
```

`payment` depends on `invoice` to load the invoice, validate that it is payable,
and mark it paid after a successful gateway charge. This keeps the validation
logic inside the invoice aggregate and avoids a cyclic module dependency.

### Domain event flow

```
POST /api/payments
        │
        ▼
PaymentService processes gateway
        │
        ▼
invoiceAPI.markAsPaid(invoiceId, amount)  (validates state + amount)
        │
        ▼
InvoicePaidEvent
        │
        ▼
notification.NotificationEventListener
        • InvoiceCreatedEvent  → "Invoice created" email
        • InvoicePaidEvent     → "Payment received" email
        • PaymentProcessedEvent(success=false) → "Payment failed" email
```

A `PaymentProcessedEvent` is still published for every payment (success or failure)
so audit consumers and failure notifications can react to it.

---

## Quick start

### Prerequisites

- Docker + Docker Compose
- (Optional) Java 21 + Maven if you prefer local builds

### Run with Docker Compose

```bash
# Build the image and start Postgres + app + Prometheus
docker compose up --build -d

# Wait for the app health check, then verify
curl http://localhost:8080/api/actuator/health
curl http://localhost:8080/api/actuator/modulith
```

Services:

| Service | URL |
|---------|-----|
| Ledgerly API | http://localhost:8080/api |
| Actuator | http://localhost:8080/api/actuator |
| Prometheus | http://localhost:9090 |
| Postgres | localhost:5432 |

When running via Docker Compose the `prod` profile is active, so business
endpoints require HTTP Basic authentication. Default credentials:

- **Username:** `ledgerly`
- **Password:** `ledgerly`

```bash
curl -u ledgerly:ledgerly http://localhost:8080/api/invoices
```

The local `dev` profile and the integration-test suite keep the API open.

### Build without running

No local Java 21/Maven is required — the build runs inside a container:

```bash
docker run --rm -v ${PWD}:/app -w /app \
  maven:3.9.9-eclipse-temurin-21 \
  mvn -B -ntp clean package
```

The resulting fat-jar is `target/ledgerly-backend-*.jar`.

---

## API overview

All endpoints are under `/api`.

### Customer module

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/customers` | Create a customer |
| `GET`  | `/customers` | List all customers |
| `GET`  | `/customers/{id}` | Get one customer |
| `PUT`  | `/customers/{id}` | Update a customer |
| `DELETE` | `/customers/{id}` | Delete a customer |

### Invoice module

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/invoices` | Create a draft invoice |
| `GET`  | `/invoices` | List invoices (optionally `?customerId=` or `?overdue=true`) |
| `GET`  | `/invoices/{id}` | Get one invoice |
| `POST` | `/invoices/{id}/issue` | Issue a draft invoice |

Invoices are **paid via the payment module**, not through an invoice endpoint.
`issueDate` is set server-side; the caller only supplies `customerId`,
`totalAmount`, `taxAmount`, and `dueDate`.

### Payment module

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/payments` | Initiate and complete a payment |
| `GET`  | `/payments` | List payments (`?invoiceId=` or `?customerId=`) |
| `GET`  | `/payments/{id}` | Get one payment |

Payment request body:

```json
{
  "invoiceId": "...",
  "customerId": "...",
  "amount": 150.00,
  "paymentMethod": "CARD",
  "transactionReference": "TXN-42"
}
```

The default gateway client approves every transaction. Replace
`PaymentGatewayClient.DefaultPaymentGatewayClient` with a real PSP client in
production.

### Reporting module

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/reports/summary` | Ledger-wide totals |
| `GET` | `/reports/customers/{id}` | Per-customer summary |

---

## Example flow

The examples below assume the `dev` profile (open endpoints). With `prod`, add
`-u ledgerly:ledgerly` to each `curl` call.

```bash
# 1. create a customer
CUSTOMER=$(curl -s -X POST http://localhost:8080/api/customers \
  -H 'Content-Type: application/json' \
  -d '{"name":"Acme Corp","email":"acme@example.com"}' | jq -r '.id')

# 2. create an invoice (due in 30 days)
DUE=$(date -d "+30 days" +%Y-%m-%d)
INVOICE=$(curl -s -X POST http://localhost:8080/api/invoices \
  -H 'Content-Type: application/json' \
  -d "{\"customerId\":\"$CUSTOMER\",\"totalAmount\":150.00,\"taxAmount\":25.50,\"dueDate\":\"$DUE\"}" | jq -r '.id')

# 3. issue it
curl -s -X POST "http://localhost:8080/api/invoices/$INVOICE/issue"

# 4. pay it
curl -s -X POST http://localhost:8080/api/payments \
  -H 'Content-Type: application/json' \
  -d "{\"invoiceId\":\"$INVOICE\",\"customerId\":\"$CUSTOMER\",\"amount\":150.00,\"paymentMethod\":\"CARD\"}"

# 5. invoice is now PAID via the event flow
curl -s "http://localhost:8080/api/invoices/$INVOICE" | jq '.status'
```

---

## Configuration profiles

| Profile | Purpose |
|---------|---------|
| `dev` (default) | SQL logging + Modulith debug logging |
| `prod` | Docker Compose profile; SQL/Modulith logging suppressed |
| `test` | H2 in-memory DB for the integration-test suite |

Runtime properties are overridable via environment variables:

```bash
LEDGERLY_DB_URL=jdbc:postgresql://postgres:5432/ledgerly
LEDGERLY_DB_USER=ledgerly
LEDGERLY_DB_PASS=ledgerly_secret
```

### Docker Compose environment

The compose file activates `prod` and overrides the datasource URL to point at
the `postgres` service.

---

## Testing

```bash
# Run tests inside Docker (H2 in-memory, no external DB)
docker run --rm -v ${PWD}:/app -w /app \
  maven:3.9.9-eclipse-temurin-21 \
  mvn -B -ntp test
```

Test coverage:

- `ModularityTests` — Spring Modulith architecture verification
- `CustomerModuleIntegrationTests`
- `InvoiceModuleIntegrationTests` — verifies `InvoiceCreatedEvent` via `Scenario`
- `PaymentModuleIntegrationTests` — end-to-end payment → invoice PAID event flow

---

## Known limitations / roadmap

These are intentionally out of scope for the current scaffold:

- **Production-grade authentication** — `prod` uses HTTP Basic with a single
  hard-coded in-memory user. Replace with JWT / OAuth2 / a real user store before
  production use.
- **Real payment gateway** — `PaymentGatewayClient.DefaultPaymentGatewayClient`
  always returns success.
- **Email delivery** — `LoggingEmailSender` only logs email metadata; integrate
  SMTP in production.

---

## License

See [LICENSE](LICENSE).
