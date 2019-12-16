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

package com.island.ohara.shabondi

import akka.http.scaladsl.model._
import com.island.ohara.common.data.Row
import com.island.ohara.kafka.Consumer
import org.junit.Test
import spray.json._

final class TestRoute extends BasicShabondiTest {
  import DefaultDefinitions._
  import JsonSupport._
  import ShabondiRouteTestSupport._

  private def sourceData: Map[String, Int] =
    (1 to 6).foldLeft(Map.empty[String, Int]) { (m, v) =>
      m + ("col-" + v -> v)
    }

  @Test
  def testSourceRoute(): Unit = {
    val topicKey1 = createTopicKey
    val config    = defaultTestConfig(SERVER_TYPE_SOURCE, Seq(topicKey1))
    val webServer = new WebServer(config)
    try {
      val requestSize = 9
      (1 to requestSize).foreach { i =>
        val jsonRow = sourceData.toJson.compactPrint
        val entity  = HttpEntity(ContentTypes.`application/json`, jsonRow)
        val request = Post(uri = "/v0", entity)

        request ~> webServer.routes ~> check {
          entityAs[String] should ===("")
        }
      }

      // assertion
      val rowsTopic1: Seq[Consumer.Record[Row, Array[Byte]]] =
        KafkaSupport.pollTopicOnce(brokerProps, topicKey1.name(), 10, 9)
      rowsTopic1.size should ===(requestSize)
      rowsTopic1(0).key.get.cells.size should ===(6)
    } finally {
      webServer.close()
      brokerClient.deleteTopic(topicKey1.name())
    }
  }
}
