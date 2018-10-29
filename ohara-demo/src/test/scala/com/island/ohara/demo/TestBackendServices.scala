package com.island.ohara.demo

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.island.ohara.client.ConfiguratorJson.PRIVATE_API
import com.island.ohara.demo.Backend._
import com.island.ohara.io.IoUtil
import com.island.ohara.rule.LargeTest
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import com.island.ohara.integration.availablePort

class TestBackendServices extends LargeTest with Matchers {

  @Test
  def testDefaultPorts(): Unit = {
    Backend.run(
      ServicePorts.default,
      (configurator, zk, brokers, workers, database, ftp) => {
        implicit val actorSystem: ActorSystem = ActorSystem(methodName)
        implicit val materializer: ActorMaterializer = ActorMaterializer()
        val result = Await.result(
          Http()
            .singleRequest(HttpRequest(HttpMethods.GET, s"http://localhost:${configurator.port}/$PRIVATE_API/services"))
            .flatMap(res => {
              if (res.status.isSuccess()) Unmarshal(res.entity).to[Services]
              else
                Future.failed(new IllegalArgumentException(s"Failed to create table. error:${res.status.intValue()}"))
            }),
          20 seconds
        )
        result.zookeeper shouldBe zk.connectionProps
        result.brokers shouldBe brokers.connectionProps
        result.workers shouldBe workers.connectionProps

        result.ftpServer.host shouldBe ftp.host
        result.ftpServer.port shouldBe ftp.port
        result.ftpServer.user shouldBe ftp.user
        result.ftpServer.password shouldBe ftp.password

        result.database.url shouldBe database.url
        result.database.user shouldBe database.user
        result.database.password shouldBe database.password
      }
    )
  }

  @Test
  def testSpecificPorts(): Unit = {
    val ports = ServicePorts(
      dbPort = availablePort(),
      ftpPort = availablePort(),
      configuratorPort = availablePort(),
      zkPort = availablePort(),
      brokersPort = Seq.fill(3)(availablePort()),
      workersPort = Seq.fill(3)(availablePort())
    )
    Backend.run(
      ports,
      (configurator, zk, brokers, workers, database, ftp) => {
        implicit val actorSystem: ActorSystem = ActorSystem(methodName)
        implicit val materializer: ActorMaterializer = ActorMaterializer()
        val result = Await.result(
          Http()
            .singleRequest(HttpRequest(HttpMethods.GET, s"http://localhost:${configurator.port}/$PRIVATE_API/services"))
            .flatMap(res => {
              if (res.status.isSuccess()) Unmarshal(res.entity).to[Services]
              else
                Future.failed(new IllegalArgumentException(s"Failed to create table. error:${res.status.intValue()}"))
            }),
          20 seconds
        )
        result.zookeeper shouldBe s"${IoUtil.hostname}:${ports.zkPort}"
        result.brokers shouldBe ports.brokersPort.map(p => s"${IoUtil.hostname}:$p").mkString(",")
        result.workers shouldBe ports.workersPort.map(p => s"${IoUtil.hostname}:$p").mkString(",")

        result.ftpServer.host shouldBe ftp.host
        result.ftpServer.port shouldBe ports.ftpPort
        result.ftpServer.user shouldBe ftp.user
        result.ftpServer.password shouldBe ftp.password

        result.database.url shouldBe database.url
        result.database.user shouldBe database.user
        result.database.password shouldBe database.password
      }
    )
  }
}
