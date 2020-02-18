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
import oharastream.ohara.common.data.{Cell, Row}
import oharastream.ohara.common.util.{CommonUtils, Releasable}
import org.junit.{After, Before, Test}

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

final class TestKafkaClient extends BasicShabondiTest {
  import oharastream.ohara.shabondi.common.ConvertSupport._

  implicit lazy val system: ActorSystem        = ActorSystem("shabondi-test")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  private var TOPIC_1: String = _

  @Before
  def before(): Unit = {
    TOPIC_1 = CommonUtils.randomString(5)
    createTestTopic(TOPIC_1)
  }

  @After
  override def tearDown(): Unit =
    topicAdmin.deleteTopic(TOPIC_1)

  @Test
  def testSingleProducer(): Unit = {
    val producer = KafkaSupport.newProducer(brokerProps)
    try {
      val row = Row.of(Cell.of("col1", 100))
      val sender = producer
        .sender()
        .key(row)
        .value(Array[Byte]())
        .topicName(TOPIC_1)

      val future = sender.send.toScala

      val metadata = Await.result(future, 3 seconds)

      metadata.topicName should ===(TOPIC_1)
      metadata.offset should ===(0)
      metadata.partition should ===(0)
    } finally {
      Releasable.close(producer)
    }
  }

  @Test
  def testConsumer(): Unit = {
    val producer = KafkaSupport.newProducer(brokerProps)
    try {
      Future.sequence {
        (1 to 9)
          .map(i => Row.of(Cell.of(s"col-$i", i * 10)))
          .map(row => producer.sender().key(row).value(Array[Byte]()).topicName(TOPIC_1))
          .map { sender =>
            sender.send.toScala
          }
      }

      val records = KafkaSupport.pollTopicOnce(brokerProps, TOPIC_1, 10, 10)

      records.size should ===(9)
      records(0).topicName == (TOPIC_1)
      records(0).key.isPresent === (true)
      records(0).key.get == (Row.of(Cell.of("col-1", 10)))

      records(8).topicName == (TOPIC_1)
      records(8).key.isPresent === (true)
      records(8).key.get == (Row.of(Cell.of("col-9", 90)))
    } finally {
      Releasable.close(producer)
    }
  }
}
