package com.island.ohara.it

import java.time.Duration

import com.island.ohara.common.data.{Cell, Row, Serializer}
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.integration.WithBroker
import com.island.ohara.kafka.{Consumer, KafkaClient, Producer}
import org.junit.Test
import org.scalatest.Matchers

import scala.collection.JavaConverters._

class TestConsumerAndProducer extends WithBroker with Matchers {

  @Test
  def testSendAndReceiveString(): Unit = {
    val topicName = methodName
    val client = KafkaClient.of(testUtil.brokersConnProps)
    try {
      if (client.exist(topicName)) client.deleteTopic(topicName)
      client.topicCreator().numberOfPartitions(1).numberOfReplications(1).compacted().create(topicName)
      CommonUtil.await(() => client.exist(topicName), Duration.ofSeconds(10))
    } finally client.close()

    val producer = Producer.builder().brokers(testUtil.brokersConnProps).build(Serializer.STRING, Serializer.STRING)
    try producer.sender().key("key").value("value").send(topicName)
    finally producer.close()

    val consumer = Consumer
      .builder()
      .topicName(topicName)
      .offsetFromBegin()
      .brokers(testUtil.brokersConnProps)
      .build(Serializer.STRING, Serializer.STRING)
    try {
      consumer.subscription().asScala.toSet shouldBe Set(topicName)
      val data = consumer.poll(java.time.Duration.ofSeconds(20), 1)
      data.get(0).value.get shouldBe "value"
    } finally consumer.close()
  }

  @Test
  def testSendAndReceiveRow(): Unit = {
    val topicName = methodName
    val data = Row.of(Cell.of("a", "abc"), Cell.of("b", 123), Cell.of("c", true))

    val client = KafkaClient.of(testUtil.brokersConnProps)
    try {
      if (client.exist(topicName)) client.deleteTopic(topicName)
      client.topicCreator().numberOfPartitions(1).numberOfReplications(1).compacted().create(topicName)
      CommonUtil.await(() => client.exist(topicName), Duration.ofSeconds(10))
    } finally client.close()

    val producer = Producer.builder().brokers(testUtil.brokersConnProps).build(Serializer.STRING, Serializer.ROW)
    try producer.sender().key("key").value(data).send(topicName)
    finally producer.close()
    val consumer = Consumer
      .builder()
      .topicName(topicName)
      .offsetFromBegin()
      .brokers(testUtil.brokersConnProps)
      .build(Serializer.STRING, Serializer.ROW)
    try {
      consumer.subscription().asScala.toSet shouldBe Set(topicName)
      val record = consumer.poll(java.time.Duration.ofSeconds(20), 1)
      record.get(0).value.get shouldBe data
    } finally consumer.close()
  }
}
