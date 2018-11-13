package com.island.ohara.it

import com.island.ohara.client.util.CloseOnce
import com.island.ohara.common.data.{Cell, Row, Serializer}
import com.island.ohara.integration.{OharaTestUtil, With3Brokers}
import com.island.ohara.kafka.{Consumer, KafkaClient, Producer}
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.duration._

class TestConsumerAndProducer extends With3Brokers with Matchers {

  @Test
  def testSendAndReceiveString(): Unit = {
    val topicName = methodName

    CloseOnce.doClose(KafkaClient(testUtil.brokersConnProps)) { client =>
      if (client.exist(topicName)) client.deleteTopic(topicName)
      client.topicCreator().numberOfPartitions(1).numberOfReplications(1).compacted().create(topicName)
      OharaTestUtil.await(() => client.exist(topicName), 10 seconds)
    }
    CloseOnce.doClose(
      Producer.builder().brokers(testUtil.brokersConnProps).build(Serializer.STRING, Serializer.STRING))(
      _.sender().key("key").value("value").send(topicName))

    CloseOnce.doClose(
      Consumer
        .builder()
        .topicName(topicName)
        .offsetFromBegin()
        .brokers(testUtil.brokersConnProps)
        .build(Serializer.STRING, Serializer.STRING)) { consumer =>
      consumer.subscription() shouldBe Set(topicName)
      val data = consumer.poll(20 seconds, 1)
      data.head.value.get shouldBe "value"
    }
  }

  @Test
  def testSendAndReceiveRow(): Unit = {
    val topicName = methodName
    val data = Row.of(Cell.of("a", "abc"), Cell.of("b", 123), Cell.of("c", true))

    CloseOnce.doClose(KafkaClient(testUtil.brokersConnProps)) { client =>
      if (client.exist(topicName)) client.deleteTopic(topicName)
      client.topicCreator().numberOfPartitions(1).numberOfReplications(1).compacted().create(topicName)
      OharaTestUtil.await(() => client.exist(topicName), 10 seconds)
    }
    CloseOnce.doClose(Producer.builder().brokers(testUtil.brokersConnProps).build(Serializer.STRING, Serializer.ROW))(
      _.sender().key("key").value(data).send(topicName))

    CloseOnce.doClose(
      Consumer
        .builder()
        .topicName(topicName)
        .offsetFromBegin()
        .brokers(testUtil.brokersConnProps)
        .build(Serializer.STRING, Serializer.ROW)) { consumer =>
      consumer.subscription() shouldBe Set(topicName)
      val record = consumer.poll(20 seconds, 1)
      record.head.value.get shouldBe data
    }
  }
}
