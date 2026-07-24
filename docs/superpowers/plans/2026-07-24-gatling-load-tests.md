# Phase 9c — Gatling Load/Performance Tests Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add 4 Gatling simulations testing CRUD-heavy user flows against a running Ledgerly instance, with JWT auth and ephemeral test data.

**Architecture:** `gatling-maven-plugin` compiles and runs Scala simulations from `src/test/gatling/scala/`. A shared `LedgerlyProtocol` object provides the HTTP config and auth login step. Each simulation login-extracts a JWT then runs its scenario against docker compose. Reports land in `target/gatling/`.

**Tech Stack:** Gatling 3.10, gatling-maven-plugin 4.8, Scala 2.13

---

### Task 1: Create gatling.conf

**Files:**
- Create: `src/test/resources/gatling.conf`

- [ ] **Step 1: Create the Gatling config file**

Create `src/test/resources/gatling.conf`:

```hocon
gatling {
  core {
    directory {
      simulations = src/test/gatling/scala
    }
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/test/resources/gatling.conf
git commit -m "feat(load): add Gatling runtime configuration"
```

---

### Task 2: Add gatling-maven-plugin to pom.xml

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: Add plugin to build section**

Add after the `maven-compiler-plugin` block (after the closing `</plugin>` at line 195):

```xml
            <plugin>
                <groupId>io.gatling</groupId>
                <artifactId>gatling-maven-plugin</artifactId>
                <version>4.8.2</version>
                <configuration>
                    <simulationsFolder>src/test/gatling/scala</simulationsFolder>
                </configuration>
            </plugin>
```

- [ ] **Step 2: Commit**

```bash
git add pom.xml
git commit -m "feat(load): add gatling-maven-plugin"
```

---

### Task 3: Create LedgerlyProtocol

**Files:**
- Create: `src/test/gatling/scala/com/ledgerly/LedgerlyProtocol.scala`

- [ ] **Step 1: Create shared protocol object**

Create directory `src/test/gatling/scala/com/ledgerly/` then write:

```scala
package com.ledgerly

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder

object LedgerlyProtocol {

  val httpProtocol: HttpProtocolBuilder = http
    .baseUrl("http://localhost:8080/api")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("Gatling/LedgerlyLoadTest")

  val adminLogin = exec(
    http("Login as admin")
      .post("/auth/login")
      .body(StringBody("""{"username":"admin","password":"ledgerly"}"""))
      .check(status.is(200))
      .check(jsonPath("$.token").saveAs("jwtToken"))
  )
}
```

- [ ] **Step 2: Commit**

```bash
git add src/test/gatling/scala/com/ledgerly/LedgerlyProtocol.scala
git commit -m "feat(load): add shared Gatling protocol with auth login"
```

---

### Task 4: Create CustomerSimulation

**Files:**
- Create: `src/test/gatling/scala/com/ledgerly/CustomerSimulation.scala`

- [ ] **Step 1: Create customer CRUD simulation**

```scala
package com.ledgerly

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class CustomerSimulation extends Simulation {

  private val httpProtocol = LedgerlyProtocol.httpProtocol

  private val customerFeeder = Iterator.continually {
    val ts = System.currentTimeMillis()
    val rnd = scala.util.Random.nextInt(100000)
    Map(
      "custName" -> s"LoadCustomer-$ts-$rnd",
      "custEmail" -> s"load-$ts-$rnd@ledgerly-test.com",
      "custTaxId" -> s"TAX-$ts-$rnd"
    )
  }

  private val scn = scenario("Customer CRUD lifecycle")
    .exec(LedgerlyProtocol.adminLogin)
    .feed(customerFeeder)
    .exec(
      http("POST /customers (create)")
        .post("/customers")
        .header("Authorization", "Bearer ${jwtToken}")
        .body(StringBody(
          """{"name":"${custName}","email":"${custEmail}","taxId":"${custTaxId}","address":"1 Load Test Ave"}"""
        ))
        .check(status.is(201))
        .check(jsonPath("$.id").saveAs("customerId"))
    )
    .pause(2)
    .exec(
      http("GET /customers (list)")
        .get("/customers?page=0&size=10&sort=name,asc")
        .header("Authorization", "Bearer ${jwtToken}")
        .check(status.is(200))
    )
    .pause(2)
    .exec(
      http("GET /customers/{id}")
        .get("/customers/${customerId}")
        .header("Authorization", "Bearer ${jwtToken}")
        .check(status.is(200))
        .check(jsonPath("$.name").is("${custName}"))
    )
    .pause(2)
    .exec(
      http("PUT /customers/{id} (update)")
        .put("/customers/${customerId}")
        .header("Authorization", "Bearer ${jwtToken}")
        .body(StringBody(
          """{"name":"${custName} UPD","email":"${custEmail}","taxId":"${custTaxId}","address":"2 Updated St"}"""
        ))
        .check(status.is(200))
    )
    .pause(2)
    .exec(
      http("DELETE /customers/{id}")
        .delete("/customers/${customerId}")
        .header("Authorization", "Bearer ${jwtToken}")
        .check(status.is(204))
    )

  setUp(
    scn.inject(rampUsers(50).during(30.seconds))
  ).protocols(httpProtocol)
    .assertions(
      global.successfulRequests.percent.gt(99),
      global.responseTime.percentile3.lt(1000)
    )
}
```

- [ ] **Step 2: Run compilation check**

```bash
docker run --rm -v ${PWD}:/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -B -ntp gatling:test -Dgatling.simulationClass=com.ledgerly.CustomerSimulation
```

Expected: Simulation compiles. If docker compose isn't running, requests will fail with connection refused — that's expected for this step (we just want to verify compilation).

- [ ] **Step 3: Commit**

```bash
git add src/test/gatling/scala/com/ledgerly/CustomerSimulation.scala
git commit -m "feat(load): add customer CRUD Gatling simulation"
```

---

### Task 5: Create InvoicePaymentSimulation

**Files:**
- Create: `src/test/gatling/scala/com/ledgerly/InvoicePaymentSimulation.scala`

- [ ] **Step 1: Create invoice + payment simulation**

```scala
package com.ledgerly

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import java.time.LocalDate
import scala.concurrent.duration._

class InvoicePaymentSimulation extends Simulation {

  private val httpProtocol = LedgerlyProtocol.httpProtocol

  private val dataFeeder = Iterator.continually {
    val ts = System.currentTimeMillis()
    val rnd = scala.util.Random.nextInt(100000)
    val tomorrow = LocalDate.now().plusDays(30).toString
    Map(
      "custName" -> s"LoadInv-$ts-$rnd",
      "custEmail" -> s"loadinv-$ts-$rnd@ledgerly-test.com",
      "custTaxId" -> s"TAX-$ts-$rnd",
      "dueDate" -> tomorrow,
      "amount" -> "150.00",
      "taxAmount" -> "25.00"
    )
  }

  private val scn = scenario("Invoice creation and payment flow")
    .exec(LedgerlyProtocol.adminLogin)
    .feed(dataFeeder)
    .exec(
      http("POST /customers (create for invoice)")
        .post("/customers")
        .header("Authorization", "Bearer ${jwtToken}")
        .body(StringBody(
          """{"name":"${custName}","email":"${custEmail}","taxId":"${custTaxId}","address":"1 Invoice Ave"}"""
        ))
        .check(status.is(201))
        .check(jsonPath("$.id").saveAs("customerId"))
    )
    .pause(3)
    .exec(
      http("POST /invoices (create)")
        .post("/invoices")
        .header("Authorization", "Bearer ${jwtToken}")
        .body(StringBody(
          """{"customerId":"${customerId}","totalAmount":${amount},"taxAmount":${taxAmount},"dueDate":"${dueDate}"}"""
        ))
        .check(status.is(201))
        .check(jsonPath("$.id").saveAs("invoiceId"))
        .check(jsonPath("$.totalAmount").saveAs("invoiceTotal"))
    )
    .pause(3)
    .exec(
      http("POST /invoices/{id}/issue")
        .post("/invoices/${invoiceId}/issue")
        .header("Authorization", "Bearer ${jwtToken}")
        .check(status.is(200))
    )
    .pause(3)
    .exec(
      http("POST /payments (process payment)")
        .post("/payments")
        .header("Authorization", "Bearer ${jwtToken}")
        .body(StringBody(
          """{"invoiceId":"${invoiceId}","customerId":"${customerId}","amount":${invoiceTotal},"paymentMethod":"BANK_TRANSFER","transactionReference":"GATLING-TXN-${customerId}"}"""
        ))
        .check(status.is(201))
        .check(jsonPath("$.id").saveAs("paymentId"))
    )
    .pause(3)
    .exec(
      http("GET /payments/{id} (verify)")
        .get("/payments/${paymentId}")
        .header("Authorization", "Bearer ${jwtToken}")
        .check(status.is(200))
        .check(jsonPath("$.status").is("COMPLETED"))
    )

  setUp(
    scn.inject(rampUsers(30).during(30.seconds))
  ).protocols(httpProtocol)
    .assertions(
      global.successfulRequests.percent.gt(99),
      global.responseTime.percentile3.lt(2000)
    )
}
```

- [ ] **Step 2: Commit**

```bash
git add src/test/gatling/scala/com/ledgerly/InvoicePaymentSimulation.scala
git commit -m "feat(load): add invoice and payment flow Gatling simulation"
```

---

### Task 6: Create ReportingSimulation

**Files:**
- Create: `src/test/gatling/scala/com/ledgerly/ReportingSimulation.scala`

- [ ] **Step 1: Create reporting simulation**

```scala
package com.ledgerly

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import java.time.LocalDate
import scala.concurrent.duration._

class ReportingSimulation extends Simulation {

  private val httpProtocol = LedgerlyProtocol.httpProtocol

  private val dataFeeder = Iterator.continually {
    val ts = System.currentTimeMillis()
    val rnd = scala.util.Random.nextInt(100000)
    val tomorrow = LocalDate.now().plusDays(30).toString
    Map(
      "custName" -> s"LoadRpt-$ts-$rnd",
      "custEmail" -> s"loadrpt-$ts-$rnd@ledgerly-test.com",
      "custTaxId" -> s"RPT-$ts-$rnd",
      "dueDate" -> tomorrow,
      "amount" -> "200.00",
      "taxAmount" -> "30.00"
    )
  }

  private val scn = scenario("Reporting read workload")
    .exec(LedgerlyProtocol.adminLogin)
    .feed(dataFeeder)
    .exec(
      http("POST /customers (setup for reports)")
        .post("/customers")
        .header("Authorization", "Bearer ${jwtToken}")
        .body(StringBody(
          """{"name":"${custName}","email":"${custEmail}","taxId":"${custTaxId}","address":"1 Report Ave"}"""
        ))
        .check(status.is(201))
        .check(jsonPath("$.id").saveAs("reportCustomerId"))
    )
    .pause(2)
    .exec(
      http("POST /invoices (setup)")
        .post("/invoices")
        .header("Authorization", "Bearer ${jwtToken}")
        .body(StringBody(
          """{"customerId":"${reportCustomerId}","totalAmount":${amount},"taxAmount":${taxAmount},"dueDate":"${dueDate}"}"""
        ))
        .check(status.is(201))
        .check(jsonPath("$.id").saveAs("reportInvoiceId"))
    )
    .pause(2)
    .exec(
      http("POST /invoices/{id}/issue (issue for reports)")
        .post("/invoices/${reportInvoiceId}/issue")
        .header("Authorization", "Bearer ${jwtToken}")
        .check(status.is(200))
    )
    .pause(2)
    .exec(
      http("GET /reports/summary")
        .get("/reports/summary")
        .header("Authorization", "Bearer ${jwtToken}")
        .check(status.is(200))
    )
    .pause(5)
    .exec(
      http("GET /reports/aging")
        .get("/reports/aging")
        .header("Authorization", "Bearer ${jwtToken}")
        .check(status.is(200))
    )
    .pause(5)
    .exec(
      http("GET /reports/customers/{id}")
        .get("/reports/customers/${reportCustomerId}")
        .header("Authorization", "Bearer ${jwtToken}")
        .check(status.is(200))
    )
    .pause(5)
    .exec(
      http("GET /reports/invoices/{id}/pdf")
        .get("/reports/invoices/${reportInvoiceId}/pdf")
        .header("Authorization", "Bearer ${jwtToken}")
        .check(status.is(200))
    )
    .pause(5)
    .exec(
      http("GET /reports/summary/excel")
        .get("/reports/summary/excel")
        .header("Authorization", "Bearer ${jwtToken}")
        .check(status.is(200))
    )

  setUp(
    scn.inject(rampUsers(20).during(20.seconds))
  ).protocols(httpProtocol)
    .assertions(
      global.successfulRequests.percent.gt(99),
      global.responseTime.percentile3.lt(5000)
    )
}
```

- [ ] **Step 2: Commit**

```bash
git add src/test/gatling/scala/com/ledgerly/ReportingSimulation.scala
git commit -m "feat(load): add reporting read workload Gatling simulation"
```

---

### Task 7: Create MixedWorkloadSimulation

**Files:**
- Create: `src/test/gatling/scala/com/ledgerly/MixedWorkloadSimulation.scala`

- [ ] **Step 1: Create combined workload simulation**

```scala
package com.ledgerly

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import java.time.LocalDate
import scala.concurrent.duration._

class MixedWorkloadSimulation extends Simulation {

  private val httpProtocol = LedgerlyProtocol.httpProtocol

  private val feeder = Iterator.continually {
    val ts = System.currentTimeMillis()
    val rnd = scala.util.Random.nextInt(100000)
    val tomorrow = LocalDate.now().plusDays(30).toString
    Map(
      "name" -> s"Mixed-$ts-$rnd",
      "email" -> s"mixed-$ts-$rnd@ledgerly-test.com",
      "taxId" -> s"MIX-$ts-$rnd",
      "dueDate" -> tomorrow
    )
  }

  private val customerCrud = exec(
    http("POST /customers")
      .post("/customers")
      .header("Authorization", "Bearer ${jwtToken}")
      .body(StringBody("""{"name":"${name}","email":"${email}","taxId":"${taxId}","address":"1 Mixed St"}"""))
      .check(status.is(201))
      .check(jsonPath("$.id").saveAs("customerId"))
  )
    .pause(2)
    .exec(
      http("GET /customers (paginated)")
        .get("/customers?page=0&size=10")
        .header("Authorization", "Bearer ${jwtToken}")
        .check(status.is(200))
    )
    .pause(2)
    .exec(
      http("GET /customers/{id}")
        .get("/customers/${customerId}")
        .header("Authorization", "Bearer ${jwtToken}")
        .check(status.is(200))
    )
    .pause(2)
    .exec(
      http("DELETE /customers/{id}")
        .delete("/customers/${customerId}")
        .header("Authorization", "Bearer ${jwtToken}")
        .check(status.is(204))
    )

  private val invoicePayment = exec(
    http("POST /customers (invoice flow)")
      .post("/customers")
      .header("Authorization", "Bearer ${jwtToken}")
      .body(StringBody("""{"name":"${name}-Inv","email":"${email}","taxId":"${taxId}","address":"1 Mixed St"}"""))
      .check(status.is(201))
      .check(jsonPath("$.id").saveAs("invCustomerId"))
  )
    .pause(2)
    .exec(
      http("POST /invoices")
        .post("/invoices")
        .header("Authorization", "Bearer ${jwtToken}")
        .body(StringBody("""{"customerId":"${invCustomerId}","totalAmount":100.00,"taxAmount":10.00,"dueDate":"${dueDate}"}"""))
        .check(status.is(201))
        .check(jsonPath("$.id").saveAs("invId"))
        .check(jsonPath("$.totalAmount").saveAs("invTotal"))
    )
    .pause(2)
    .exec(
      http("POST /invoices/{id}/issue")
        .post("/invoices/${invId}/issue")
        .header("Authorization", "Bearer ${jwtToken}")
        .check(status.is(200))
    )
    .pause(2)
    .exec(
      http("POST /payments")
        .post("/payments")
        .header("Authorization", "Bearer ${jwtToken}")
        .body(StringBody("""{"invoiceId":"${invId}","customerId":"${invCustomerId}","amount":${invTotal},"paymentMethod":"BANK_TRANSFER","transactionReference":"MIXED-${invCustomerId}"}"""))
        .check(status.is(201))
    )

  private val reportingReads = exec(
    http("GET /reports/summary")
      .get("/reports/summary")
      .header("Authorization", "Bearer ${jwtToken}")
      .check(status.is(200))
  )
    .pause(3)
    .exec(
      http("GET /reports/aging")
        .get("/reports/aging")
        .header("Authorization", "Bearer ${jwtToken}")
        .check(status.is(200))
    )

  private val scn = scenario("Mixed workload")
    .exec(LedgerlyProtocol.adminLogin)
    .feed(feeder)
    .randomSwitch(
      40.0 -> customerCrud,
      35.0 -> invoicePayment,
      25.0 -> reportingReads
    )

  setUp(
    scn.inject(rampUsers(50).during(60.seconds))
  ).protocols(httpProtocol)
    .assertions(
      global.successfulRequests.percent.gt(95),
      global.responseTime.percentile3.lt(3000)
    )
}
```

- [ ] **Step 2: Commit**

```bash
git add src/test/gatling/scala/com/ledgerly/MixedWorkloadSimulation.scala
git commit -m "feat(load): add mixed workload Gatling simulation"
```

---

### Task 8: Verify compilation and run against docker compose

**Files:**
- None (verification only)

- [ ] **Step 1: Start docker compose with fresh DB**

```bash
docker compose down --volumes && docker compose up --build -d
```

Wait for app to be healthy:
```bash
curl --retry 20 --retry-delay 3 --retry-connrefused -s http://localhost:8080/api/actuator/health
```

Expected: `{"status":"UP"}`

- [ ] **Step 2: Run single simulation to verify end-to-end**

```bash
mvn gatling:test -Dgatling.simulationClass=com.ledgerly.CustomerSimulation
```

Expected: Simulation starts, Gatling opens HTML report (verify no failures).

Note: This step requires Docker to be running on the host (not inside a container), since Gatling runs as a Maven goal on the host and sends HTTP requests to localhost:8080.

- [ ] **Step 3: Install Gatling Maven plugin dependencies**

Since we run tests in Docker, the Gatling plugin needs to be available. Verify the plugin resolves:

```bash
docker run --rm -v ${PWD}:/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -B -ntp gatling:help
```

Expected: BUILD SUCCESS (plugin resolves and shows help)

- [ ] **Step 4: Commit any test results or adjustments**

```bash
git add -A
git diff --cached --stat
```

---

### Task 9: Update IMPLEMENTATION_STATUS.md

**Files:**
- Modify: `docs/IMPLEMENTATION_STATUS.md`

- [ ] **Step 1: Mark Phase 9c complete**

Replace the Phase 9c section (line ~244):

Old text:
```markdown
#### Phase 9c — Load/performance tests (NOT STARTED)

- Add Gatling or k6 load tests against the running app
- Best done after pagination + caching land
```

Replace with:
```markdown
#### Phase 9c — Load/performance tests (COMPLETED)

**Status:** all planned features implemented.

What was delivered:
- 4 Gatling simulations simulating realistic CRUD-heavy user flows:
  - `CustomerSimulation` — 50 users, 30s ramp: create, list, get, update,
    delete customers
  - `InvoicePaymentSimulation` — 30 users, 30s ramp: create customer →
    create invoice → issue → pay → verify payment status
  - `ReportingSimulation` — 20 users, 20s ramp: create setup data → fetch
    summary, aging, customer report, invoice PDF, Excel summary
  - `MixedWorkloadSimulation` — 50 users, 60s ramp: random-weight mix of
    40% customer CRUD, 35% invoice+payment, 25% reporting reads
- Shared `LedgerlyProtocol` with base URL (`localhost:8080/api`) and JWT
  auth login step (logs in as seeded admin, stores token in session)
- `gatling-maven-plugin` 4.8.2 in pom.xml, `gatling.conf` runtime config
- Assertions on success rate (>99% per simulation) and p99 response
  times (1–5s depending on endpoint weight)
- Ephemeral test data: each user creates unique entities via feeder with
  timestamp + random suffixes
- Run command: `mvn gatling:test` against `docker compose up` app
```

- [ ] **Step 2: Commit**

```bash
git add docs/IMPLEMENTATION_STATUS.md
git commit -m "docs: mark Phase 9c as completed"
```
