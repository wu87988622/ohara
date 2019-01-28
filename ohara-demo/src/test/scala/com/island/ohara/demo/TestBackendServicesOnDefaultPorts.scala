/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.island.ohara.demo

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.island.ohara.client.configurator.ConfiguratorApiInfo
import com.island.ohara.common.rule.LargeTest
import com.island.ohara.demo.Backend._
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
@deprecated("embedded services are deprecated. We all should love docker, shouldn't we?", "0.2")
class TestBackendServicesOnDefaultPorts extends LargeTest with Matchers {

  @Test
  def test(): Unit = {
    Backend.run(
      ServicePorts.default,
      (configurator, zk, brokers, workers, database, ftp) => {
        implicit val actorSystem: ActorSystem = ActorSystem(methodName)
        implicit val materializer: ActorMaterializer = ActorMaterializer()
        val result = Await.result(
          Http()
            .singleRequest(
              HttpRequest(HttpMethods.GET,
                          s"http://localhost:${configurator.port}/${ConfiguratorApiInfo.PRIVATE}/services"))
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

        result.ftpServer.hostname shouldBe ftp.hostname
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
