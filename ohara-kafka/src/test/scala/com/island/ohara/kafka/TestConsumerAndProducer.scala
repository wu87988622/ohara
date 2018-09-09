package com.island.ohara.kafka

import com.island.ohara.data.{Cell, Row}
import com.island.ohara.integration.{OharaTestUtil, With3Brokers}
import com.island.ohara.io.CloseOnce
import com.island.ohara.serialization.Serializer._
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.duration._

class TestConsumerAndProducer extends With3Brokers with Matchers {
  private[this] val data = Row(Cell("a", "abc"), Cell("b", 123), Cell("c", true))

  @Test
  def testSendAndReceiveString(): Unit = {
    val topicName = methodName

    CloseOnce.doClose(KafkaClient(testUtil.brokers)) { client =>
      if (client.exist(topicName)) client.deleteTopic(topicName)
      client.topicCreator().numberOfPartitions(1).numberOfReplications(1).compacted().create(topicName)
      OharaTestUtil.await(() => client.exist(topicName), 10 seconds)
    }
    CloseOnce.doClose(Producer.builder().brokers(testUtil.brokers).build[String, String])(
      _.sender().key("key").value("value").send(topicName))

    CloseOnce.doClose(
      Consumer.builder().topicName(topicName).offsetFromBegin().brokers(testUtil.brokers).build[String, String]) {
      consumer =>
        val data = consumer.poll(20 seconds, 1)
        data.head.value.get shouldBe "value"
    }
  }

}
