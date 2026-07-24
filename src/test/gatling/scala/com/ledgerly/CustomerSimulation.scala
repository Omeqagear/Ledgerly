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
