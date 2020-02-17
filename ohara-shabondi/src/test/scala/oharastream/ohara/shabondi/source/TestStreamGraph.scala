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

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import oharastream.ohara.common.data.Row
import oharastream.ohara.common.util.Releasable
import oharastream.ohara.kafka.Consumer
import oharastream.ohara.shabondi.{BasicShabondiTest, KafkaSupport}
import org.junit.Test

import scala.concurrent.Await
import scala.concurrent.duration._

final class TestStreamGraph extends BasicShabondiTest {
  implicit lazy val system: ActorSystem        = ActorSystem("shabondi-stream-graph")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  import system.dispatcher

  @Test
  def testFromSendRow(): Unit = {
    val topicKey1 = createTopicKey
    val topicKey2 = createTopicKey
    val producer  = KafkaSupport.newProducer(brokerProps)
    try {
      val columnSize = 10
      val topics     = Seq(topicKey1, topicKey2)
      val row        = singleRow(columnSize, 1)

      // Send row to two topics
      val future = StreamGraph.fromSendRow(producer, topics, row).run()

      Await.result(future, Duration.Inf)

      // assertion
      val rowsTopic1: Seq[Consumer.Record[Row, Array[Byte]]] =
        KafkaSupport.pollTopicOnce(brokerProps, topicKey1.topicNameOnKafka, 10, columnSize)
      rowsTopic1.size should ===(1)
      rowsTopic1(0).key.get.cells.size should ===(columnSize)

      val rowsTopic2: Seq[Consumer.Record[Row, Array[Byte]]] =
        KafkaSupport.pollTopicOnce(brokerProps, topicKey2.topicNameOnKafka, 10, columnSize)

      rowsTopic2.size should ===(1)
      rowsTopic2(0).key.get.cells.size should ===(columnSize)
    } finally {
      Releasable.close(producer)
      topicAdmin.deleteTopic(topicKey1)
      topicAdmin.deleteTopic(topicKey2)
    }
  }
}
