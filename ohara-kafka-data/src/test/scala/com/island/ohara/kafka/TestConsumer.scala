package com.island.ohara.kafka

import java.util.Properties

import com.island.ohara.rule.SmallTest
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.junit.Test
import org.scalatest.Matchers

class TestConsumer extends SmallTest with Matchers {

  @Test
  def testCreateTableConsumer():Unit = {
    val producerProps = new Properties
    producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9000")
    new TableConsumer[Array[Byte]](producerProps, new ByteArrayDeserializer).close()
  }

  @Test
  def testCreateRowConsumer():Unit = {
    val producerProps = new Properties
    producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9000")
    new RowConsumer[Array[Byte]](producerProps, new ByteArrayDeserializer).close()
  }
}
