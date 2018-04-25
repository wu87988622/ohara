package com.island.ohara.kafka

import java.util.Properties

import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class TestConsumer extends FlatSpec with Matchers {

  "Newing a TableConsumer" should "work" in {
    val producerProps = new Properties
    producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9000")
    new TableConsumer[Array[Byte]](producerProps, new ByteArrayDeserializer).close()
  }

  "Newing a RowConsumer" should "work" in {
    val producerProps = new Properties
    producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9000")
    new RowConsumer[Array[Byte]](producerProps, new ByteArrayDeserializer).close()
  }
}
