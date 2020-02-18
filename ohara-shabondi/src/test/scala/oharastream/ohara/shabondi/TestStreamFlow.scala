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

package oharastream.ohara.shabondi

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.{Done, NotUsed}
import oharastream.ohara.common.data.{Cell, Row}
import oharastream.ohara.common.util.Releasable
import oharastream.ohara.kafka.{Consumer, RecordMetadata}
import org.junit.Test

import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

final class TestStreamFlow extends BasicShabondiTest {
  import oharastream.ohara.shabondi.common.ConvertSupport._

  implicit lazy val system: ActorSystem        = ActorSystem("shabondi-test")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  import system.dispatcher

  @Test
  def testSimple(): Unit = {
    val source = Source(1 to 10)
    val sink   = Sink.fold(0)((a: Int, b: Int) => a + b)

    val futureSum = source.toMat(sink)(Keep.right).run()
    val sum       = Await.result(futureSum, 2 seconds)
    sum should ===(55)
  }

  @Test
  def testSingleSource(): Unit = {
    val topicKey1 = createTopicKey
    val singleRow = Row.of(
      (1 to 10).map(i => Cell.of(s"col-$i", i * 10)): _*
    )
    val producer = KafkaSupport.newProducer(brokerProps)
    try {
      singleRow.cells().size should ===(10)
      val source: Source[Row, NotUsed] = Source.single(singleRow)
      val pushRow: Flow[Row, RecordMetadata, NotUsed] = Flow[Row].mapAsync(4) { row =>
        val sender = producer.sender().key(row).topicName(topicKey1.topicNameOnKafka)
        sender.send.toScala
      }

      val sink = Sink.foreach[RecordMetadata] { el =>
        println(s"metadata: $el")
      }

      val future: Future[Done] = source.via(pushRow).toMat(sink)(Keep.right).run()
      Await.result(future, 2 seconds)

      val rows = KafkaSupport.pollTopicOnce(brokerProps, topicKey1.topicNameOnKafka, 10, 10)
      rows.size should ===(1)
    } finally {
      Releasable.close(producer)
      topicAdmin.deleteTopic(topicKey1)
    }
  }

  @Test
  def testMultipleTopics(): Unit = {
    val topicKey1  = createTopicKey
    val topicKey2  = createTopicKey
    val topicKeys  = Seq(topicKey1, topicKey2).to[immutable.Iterable]
    val producer   = KafkaSupport.newProducer(brokerProps)
    val maxRowSize = 1000
    val rows       = multipleRows(maxRowSize)
    try {
      val source: Source[Row, NotUsed] = Source(rows)

      val flowSendRow = Flow[Row].mapAsync(4) { row =>
        Future.sequence(topicKeys.map { topicKey =>
          val sender = producer.sender().key(row).topicName(topicKey.topicNameOnKafka)
          sender.send.toScala
        })
      } //.log("flowSendRow")

      log.info("=== send rows flow start ===")

      val sink   = Sink.ignore
      val future = source.via(flowSendRow).toMat(sink)(Keep.right).run()
      Await.result(future, Duration.Inf)

      log.info("=== send rows flow finish ===")

      // assertion
      val rowsTopic1: Seq[Consumer.Record[Row, Array[Byte]]] =
        KafkaSupport.pollTopicOnce(brokerProps, topicKey1.topicNameOnKafka, 30, maxRowSize)
      rowsTopic1.size should ===(maxRowSize)

      val rowsTopic2: Seq[Consumer.Record[Row, Array[Byte]]] =
        KafkaSupport.pollTopicOnce(brokerProps, topicKey2.topicNameOnKafka, 30, maxRowSize)
      rowsTopic2.size should ===(maxRowSize)
    } finally {
      Releasable.close(producer)
      topicAdmin.deleteTopic(topicKey1)
      topicAdmin.deleteTopic(topicKey2)
    }
  }

  @Test
  def testMultipleRow(): Unit = {
    val topicKey1  = createTopicKey
    val producer   = KafkaSupport.newProducer(brokerProps)
    val maxRowSize = 100
    val rows       = multipleRows(maxRowSize)
    try {
      val source: Source[Row, NotUsed] = Source(rows)
      val sendRow: Flow[Row, RecordMetadata, NotUsed] = Flow[Row].mapAsync(4) { row =>
        val sender = producer.sender().key(row).topicName(topicKey1.topicNameOnKafka)
        sender.send.toScala
      } //.log("pushRow")

      val sink = Sink.ignore

      val future: Future[Done] = source.via(sendRow).toMat(sink)(Keep.right).run()
      Await.result(future, 10 seconds)

      // assertion
      val rowsTopic1: Seq[Consumer.Record[Row, Array[Byte]]] =
        KafkaSupport.pollTopicOnce(brokerProps, topicKey1.topicNameOnKafka, 30, maxRowSize)
      rowsTopic1.size should ===(maxRowSize)

      rowsTopic1(0).key.get.cell(0) should ===(Cell.of("col-1", "r0-10"))
      rowsTopic1(99).key.get.cell(9) should ===(Cell.of("col-10", "r99-100"))
    } finally {
      Releasable.close(producer)
      topicAdmin.deleteTopic(topicKey1)
    }
  }
}
