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

package oharastream.ohara.shabondi.source

import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.RouteTestTimeout
import oharastream.ohara.common.data.Row
import oharastream.ohara.kafka.Consumer
import oharastream.ohara.metrics.BeanChannel
import oharastream.ohara.metrics.basic.CounterMBean
import oharastream.ohara.shabondi.{BasicShabondiTest, KafkaSupport}
import org.junit.Test
import spray.json._

import scala.concurrent.duration._
import scala.collection.JavaConverters._

final class TestSourceRoute extends BasicShabondiTest {
  import oharastream.ohara.shabondi.ShabondiRouteTestSupport._
  import oharastream.ohara.shabondi.common.JsonSupport._

  // Extend the timeout to avoid the exception:
  // org.scalatest.exceptions.TestFailedException: Request was neither completed nor rejected within 1 second
  implicit val routeTestTimeout = RouteTestTimeout(5 seconds)

  private def sourceData: Map[String, Int] =
    (1 to 6).foldLeft(Map.empty[String, Int]) { (m, v) =>
      m + ("col-" + v -> v)
    }

  @Test
  def testSourceRoute(): Unit = {
    val topicKey1 = createTopicKey
    val config    = defaultSourceConfig(Seq(topicKey1))
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
        KafkaSupport.pollTopicOnce(brokerProps, topicKey1.topicNameOnKafka, 10, 9)
      rowsTopic1.size should ===(requestSize)
      rowsTopic1(0).key.get.cells.size should ===(6)

      // assert metrics
      val beans = counterMBeans()
      beans.size should ===(1)
      beans(0).getValue should ===(requestSize)
    } finally {
      webServer.close()
      topicAdmin.deleteTopic(topicKey1)
    }
  }

  private def counterMBeans(): Seq[CounterMBean] = BeanChannel.local().counterMBeans().asScala
}
