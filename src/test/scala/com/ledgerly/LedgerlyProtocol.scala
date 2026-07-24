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
