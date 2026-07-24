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
