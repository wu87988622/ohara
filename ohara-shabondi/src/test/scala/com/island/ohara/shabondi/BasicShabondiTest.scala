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

import java.time.Duration

import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.island.ohara.common.data.{Cell, Row}
import com.island.ohara.common.setting.TopicKey
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.kafka.{BrokerClient, Consumer}
import com.island.ohara.shabondi.DefaultDefinitions._
import com.island.ohara.testing.WithBroker
import com.typesafe.scalalogging.Logger
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, Suite}

import scala.collection.JavaConverters._
import scala.collection.immutable

object ShabondiRouteTestSupport extends Suite with ScalaFutures with ScalatestRouteTest

abstract class BasicShabondiTest extends WithBroker with Matchers {
  protected val log = Logger(this.getClass())

  protected val brokerProps                = testUtil.brokersConnProps
  protected val brokerClient: BrokerClient = BrokerClient.of(brokerProps)

  protected def createTopicKey = TopicKey.of("default", CommonUtils.randomString(5))

  protected def createTestTopic(name: String): Unit =
    brokerClient.topicCreator
      .numberOfPartitions(1)
      .numberOfReplications(1.toShort)
      .topicName(name)
      .create

  protected def defaultTestConfig(serverType: String, topicKeys: Seq[TopicKey]): Config = {
    val jsonToTopics = TopicKey.toJsonString(topicKeys.asJava)
    //val jsonFromTopic = TopicKey.toJsonString(topicKey2)
    val args = Array(
      s"$SERVER_TYPE_KEY=$serverType",
      s"$CLIENT_PORT_KEY=8080",
      s"$BROKERS_KEY=${testUtil.brokersConnProps}",
      s"$SOURCE_TO_TOPICS_KEY=$jsonToTopics"
      //s"$SINK_FROM_TOPIC_KEY=$jsonFromTopic"
    )
    val rawConfig = CommonUtils.parse(args.toSeq.asJava)
    Config(rawConfig.asScala.toMap)
  }

  protected def singleRow(columnSize: Int, rowId: Int = 0) =
    Row.of(
      (1 to columnSize).map(ci => Cell.of(s"col-$ci", s"r$rowId-${ci * 10}")): _*
    )

  protected def multipleRows(rowSize: Int): immutable.Iterable[Row] =
    (0 until rowSize).map { ri =>
      singleRow(10, ri)
    }

  protected def pollTopicOnce(
    brokers: String,
    topicName: String,
    timeoutSecond: Long,
    expectedSize: Int
  ): Seq[Consumer.Record[Row, Array[Byte]]] = {
    val consumer = KafkaClient.newConsumer(brokers, topicName)
    try {
      consumer.poll(Duration.ofSeconds(timeoutSecond), expectedSize).asScala
    } finally {
      Releasable.close(consumer)
    }
  }
}
