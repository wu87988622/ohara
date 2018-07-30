package com.island.ohara.source.http

import akka.testkit.TestKit
import com.island.ohara.rule.SmallTest
import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import com.island.ohara.integration.OharaTestUtil
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.CommonClientConfigs
import org.junit.{After, Test}
import org.scalatest.Matchers
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.{ScalaFutures, ScaledTimeSpans}
import org.scalatest.time.{Seconds, Span}
import org.scalatest.time.SpanSugar._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer

import scala.concurrent.Await

class TestHttpConnectorActorWithMiniCluster
    extends TestKit(ActorSystem("testing", ConfigFactory.load()))
    with Matchers
    with ScaledTimeSpans
    with SmallTest {

  private val testUtil = OharaTestUtil.localBrokers(3)
  private implicit val materializer = ActorMaterializer()
  private val interface = "localhost"
  private val port = 5566
  private val serverIPs =
    testUtil.producerConfig.get(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG).get.left.get.split(",")
  private val configStr =
    s"""
       |http {
       |  interface = "${interface}"
       |  port = ${port}
       |}
       |
       |bootstrap.servers = [
       | ${serverIPs.map(server => "\"" + server + "\"").mkString(",")}
       |]
      """.stripMargin

  private val actorName = "HttpConnectorActor"

  private def getNewHttpActor = system.actorOf(Props(new HttpConnectorActor), name = actorName)

  @Test
  def startHttpServerShouldReturnHealthCheck(): Unit = {

    val httpActor = getNewHttpActor

    val config = ConfigFactory.parseString(configStr)
    httpActor ! HttpCommand.START(config)

    val futureResponse = Http().singleRequest(HttpRequest(uri = "http://" + interface + ":" + port))

    ScalaFutures.whenReady(futureResponse, timeout = Timeout(Span(2, Seconds))) { response =>
      response.status shouldBe StatusCodes.OK
      Unmarshal(response.entity).to[String]
      Await.result(Unmarshal(response.entity).to[String], 1 second) shouldBe "Alive"
    }

    httpActor ! HttpCommand.STOP

    httpActor ! PoisonPill
  }

  @Test
  def stopHttpServerShouldFail(): Unit = {

    val httpActor = getNewHttpActor

    val config = ConfigFactory.parseString(configStr)
    httpActor ! HttpCommand.START(config)
    httpActor ! HttpCommand.STOP

    val futureResponse = Http().singleRequest(HttpRequest(uri = "http://" + interface + ":" + port))

    assertThrows[Exception] {
      Await.result(futureResponse, 1 seconds)
    }

    httpActor ! PoisonPill
  }

  @Test
  def stopAndStartAgainShouldSuccess(): Unit = {

    val httpActor = getNewHttpActor

    val config = ConfigFactory.parseString(configStr)
    httpActor ! HttpCommand.START(config)
    httpActor ! HttpCommand.STOP
    httpActor ! HttpCommand.START(config)

    val futureResponse = Http().singleRequest(HttpRequest(uri = "http://" + interface + ":" + port))

    ScalaFutures.whenReady(futureResponse, timeout = Timeout(Span(5, Seconds))) { response =>
      response.status shouldBe StatusCodes.OK
      Unmarshal(response.entity).to[String]
      Await.result(Unmarshal(response.entity).to[String], 1 second) shouldBe "Alive"
    }

    httpActor ! HttpCommand.STOP

    httpActor ! PoisonPill
  }

  @After
  def tearDown(): Unit = {
    TestKit.shutdownActorSystem(system)
  }
}
