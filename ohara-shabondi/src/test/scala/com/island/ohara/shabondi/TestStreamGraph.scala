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

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.island.ohara.common.data.Row
import com.island.ohara.common.util.Releasable
import com.island.ohara.kafka.Consumer
import org.junit.Test

import scala.concurrent.Await
import scala.concurrent.duration.Duration

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
        pollTopicOnce(brokerProps, topicKey1.name(), 10, columnSize)
      rowsTopic1.size should ===(1)
      rowsTopic1(0).key.get.cells.size should ===(columnSize)

      val rowsTopic2: Seq[Consumer.Record[Row, Array[Byte]]] =
        pollTopicOnce(brokerProps, topicKey2.name(), 10, columnSize)

      rowsTopic2.size should ===(1)
      rowsTopic2(0).key.get.cells.size should ===(columnSize)
    } finally {
      Releasable.close(producer)
      brokerClient.deleteTopic(topicKey1.name())
      brokerClient.deleteTopic(topicKey2.name())
    }
  }
}
