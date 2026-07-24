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
