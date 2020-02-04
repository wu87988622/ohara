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

package com.island.ohara.client.kafka

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.{Http, server}
import akka.stream.ActorMaterializer
import com.island.ohara.client.kafka.WorkerJson.{ConnectorCreationResponse, KafkaConnectorTaskId, _}
import com.island.ohara.common.rule.OharaTest
import com.island.ohara.common.setting.ConnectorKey
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.kafka.connector.json.Creation
import org.junit.Test
import org.scalatest.Matchers._

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * https://github.com/oharastream/ohara/issues/873.
  */
class Test873 extends OharaTest {
  @Test
  def testCreateConnector(): Unit = {
    val className = CommonUtils.randomString()
    val settings = Map(
      CommonUtils.randomString() -> CommonUtils.randomString()
    )
    val tasks = Seq(
      KafkaConnectorTaskId(
        connector = CommonUtils.randomString(),
        task = 10
      )
    )
    val server = toServer {
      path("connectors") {
        post {
          entity(as[Creation]) { req =>
            complete(
              ConnectorCreationResponse(
                name = req.name(),
                config = req.configs().asScala.toMap,
                tasks = tasks
              )
            )
          }
        }
      }
    }

    try {
      val connectorKey = ConnectorKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
      val client       = ConnectorAdmin(s"${server.hostname}:${server.port}")
      val response = result(
        client.connectorCreator().connectorKey(connectorKey).settings(settings).className(className).create()
      )
      response.name shouldBe connectorKey.connectorNameOnKafka()
      response.tasks shouldBe tasks
      settings.foreach {
        case (k, v) =>
          response.config(k) shouldBe v
      }
    } finally server.close()
  }

  private[this] def toServer(route: server.Route): SimpleServer = {
    implicit val system: ActorSystem             = ActorSystem("my-system")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    val server                                   = Await.result(Http().bindAndHandle(route, "localhost", 0), 30 seconds)
    new SimpleServer {
      override def hostname: String = server.localAddress.getHostString
      override def port: Int        = server.localAddress.getPort
      override def close(): Unit = {
        Await.result(server.unbind(), 30 seconds)
        Await.result(system.terminate(), 30 seconds)
      }
    }
  }
}
