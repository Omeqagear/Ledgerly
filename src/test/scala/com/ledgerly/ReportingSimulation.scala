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
