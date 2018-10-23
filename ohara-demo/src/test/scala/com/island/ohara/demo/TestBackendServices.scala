package com.island.ohara.demo

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.island.ohara.client.ConfiguratorJson.PRIVATE_API
import com.island.ohara.demo.Backend._
import com.island.ohara.rule.LargeTest
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TestBackendServices extends LargeTest with Matchers {

  @Test
  def testServicesApi(): Unit = {
    Backend.run(
      0,
      (configurator, database, ftp) => {
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
}
