package com.island.ohara.manager

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.MalformedRequestContentRejection
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.island.ohara.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers
import org.scalatest.concurrent.ScalaFutures
import spray.json.DeserializationException

class TestApiRoutes extends SmallTest with Matchers with ScalaFutures with ScalatestRouteTest {
  @Test
  def testLoginSuccess(): Unit = {
    val apiRoutes = new ApiRoutes(system, null)
    val request = HttpRequest(
      method = HttpMethods.POST,
      uri = "/api/login",
      entity = HttpEntity(MediaTypes.`application/json`, "{\"name\": \"jack\", \"password\": \"jack123\"}"))

    request ~> apiRoutes.routes ~> check {
      status should ===(StatusCodes.OK)
      contentType should ===(ContentTypes.`application/json`)
      entityAs[String] should ===("""{"data":true,"message":"login success"}""")
    }
  }

  @Test
  def testLoginFail1(): Unit = {
    val apiRoutes = new ApiRoutes(system, null)
    val request = HttpRequest(
      method = HttpMethods.POST,
      uri = "/api/login",
      entity = HttpEntity(MediaTypes.`application/json`, "{\"name\": \"jack\", \"password\": \"jack12345\"}"))

    request ~> apiRoutes.routes ~> check {
      status should ===(StatusCodes.OK)
      contentType should ===(ContentTypes.`application/json`)
      entityAs[String] should ===("""{"data":false,"message":"login fail"}""")
    }
  }

  @Test
  def testLoginFail2(): Unit = {
    val apiRoutes = new ApiRoutes(system, null)
    val request = HttpRequest(method = HttpMethods.POST,
                              uri = "/api/login",
                              entity = HttpEntity(MediaTypes.`application/json`, "{}"))

    request ~> apiRoutes.routes ~> check {
      rejection.toString() shouldEqual
        MalformedRequestContentRejection(
          "Object is missing required member 'name'",
          DeserializationException("Object is missing required member 'name'")).toString()
    }
  }

  @Test
  def testLogoutFailed: Unit = {
    val apiRoutes = new ApiRoutes(system, null)
    val requestLogout = HttpRequest(method = HttpMethods.POST,
                                    uri = "/api/logout",
                                    entity = HttpEntity(MediaTypes.`application/json`, "jack"))

    requestLogout ~> apiRoutes.routes ~> check {
      status should ===(StatusCodes.OK)
      contentType should ===(ContentTypes.`application/json`)
      entityAs[String] should ===("""{"data":false,"message":"logout fail."}""")
    }
  }
}
