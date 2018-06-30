package com.island.ohara.manager

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.island.ohara.config.{OharaConfig, OharaJson}
import com.island.ohara.configurator.Configurator
import com.island.ohara.configurator.data.OharaSchema
import com.island.ohara.io.CloseOnce
import com.island.ohara.io.CloseOnce.doClose
import com.island.ohara.rest.RestClient
import com.island.ohara.rule.SmallTest
import com.island.ohara.serialization.BYTES
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.Await
import scala.concurrent.duration._

class TestApiRoutesWithConfigurator extends SmallTest with Matchers {

  private[this] val configurator = Configurator.builder.inMemoryStore().hostname("localhost").port(0).build()
  private[this] val restClient = RestClient()

  private[this] def toMap(body: String) = OharaConfig(OharaJson(body))
  private[this] implicit val system = ActorSystem("TestApiRoutesWithConfigurator")
  private[this] implicit val materializer = ActorMaterializer()

  @Test
  def testSendSchemaRequest(): Unit = {

    doClose(new ApiRoutes(system, (configurator.hostname, configurator.port))) { route =>
      {
        val httpServer: Http.ServerBinding =
          Await.result(Http().bindAndHandle(route.routes, "localhost", 0), 10 seconds)
        // add an new schema
        val types = Map("cf" -> BYTES)
        val orders = Map("cf" -> 1)
        val name = "name"
        var rsp = restClient.post(httpServer.localAddress.getHostName,
                                  httpServer.localAddress.getPort,
                                  "api/schemas",
                                  OharaSchema.json(name, types, orders, true))
        rsp.statusCode shouldBe 200
        toMap(rsp.body).requireString("status").toBoolean shouldBe true
        val uuid = toMap(rsp.body).requireString("uuid")
        uuid shouldBe configurator.iterator.next().uuid

        // list the schema
        rsp = restClient.get(httpServer.localAddress.getHostName, httpServer.localAddress.getPort, "api/schemas")
        rsp.statusCode shouldBe 200
        toMap(rsp.body).requireString("status").toBoolean shouldBe true
        toMap(rsp.body).requireMap("uuids").get(uuid).get shouldBe name

        // get the schema
        rsp = restClient.get(httpServer.localAddress.getHostName, httpServer.localAddress.getPort, s"api/schemas/$uuid")
        rsp.statusCode shouldBe 200
        toMap(rsp.body).requireString("status").toBoolean shouldBe true
        val returnedSchema = OharaSchema(OharaJson(rsp.body))
        returnedSchema.uuid shouldBe uuid
        returnedSchema.name shouldBe name
        returnedSchema.types.sameElements(types) shouldBe true
        returnedSchema.orders.sameElements(orders) shouldBe true
        returnedSchema.disabled shouldBe true

        // get a nonexistant schema
        rsp = restClient.get(httpServer.localAddress.getHostName, httpServer.localAddress.getPort, s"api/schemas/xxx")
        rsp.statusCode shouldBe 200
        toMap(rsp.body).requireString("status").toBoolean shouldBe false

        // delete the schema
        rsp =
          restClient.delete(httpServer.localAddress.getHostName, httpServer.localAddress.getPort, s"api/schemas/$uuid")
        rsp.statusCode shouldBe 200
        toMap(rsp.body).requireString("status").toBoolean shouldBe true

        // delete a nonexistant schema
        rsp =
          restClient.delete(httpServer.localAddress.getHostName, httpServer.localAddress.getPort, s"api/schemas/$uuid")
        rsp.statusCode shouldBe 200
        toMap(rsp.body).requireString("status").toBoolean shouldBe false

        Await.result(httpServer.unbind(), 10 seconds)
      }
    }
  }

  @After
  def tearDown(): Unit = {
    CloseOnce.close(restClient)
    CloseOnce.close(configurator)
    Await.result(system.terminate(), 10 seconds)
  }
}
