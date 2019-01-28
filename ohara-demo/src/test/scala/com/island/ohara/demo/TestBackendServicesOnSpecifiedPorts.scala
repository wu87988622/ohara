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
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.demo.Backend._
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
@deprecated("embedded services are deprecated. We all should love docker, shouldn't we?", "0.2")
class TestBackendServicesOnSpecifiedPorts extends LargeTest with Matchers {

  @Test
  def test(): Unit = {
    val ports = ServicePorts(
      dbPort = CommonUtil.availablePort(),
      ftpPort = CommonUtil.availablePort(),
      ftpDataPorts = Seq(CommonUtil.availablePort()),
      configuratorPort = CommonUtil.availablePort(),
      zkPort = CommonUtil.availablePort(),
      brokersPort = Seq.fill(3)(CommonUtil.availablePort()).toArray,
      workersPort = Seq.fill(3)(CommonUtil.availablePort()).toArray
    )
    Backend.run(
      ports,
      (configurator, _, _, _, database, ftp) => {
        implicit val actorSystem: ActorSystem = ActorSystem(methodName)
        try {
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
          result.zookeeper shouldBe s"${CommonUtil.hostname}:${ports.zkPort}"
          result.brokers shouldBe ports.brokersPort.map(p => s"${CommonUtil.hostname}:$p").mkString(",")
          result.workers shouldBe ports.workersPort.map(p => s"${CommonUtil.hostname}:$p").mkString(",")

          result.ftpServer.hostname shouldBe ftp.hostname
          result.ftpServer.port shouldBe ports.ftpPort
          result.ftpServer.dataPort shouldBe ports.ftpDataPorts
          result.ftpServer.user shouldBe ftp.user
          result.ftpServer.password shouldBe ftp.password

          result.database.url shouldBe database.url
          result.database.user shouldBe database.user
          result.database.password shouldBe database.password
        } finally actorSystem.terminate()
      }
    )
  }
}
