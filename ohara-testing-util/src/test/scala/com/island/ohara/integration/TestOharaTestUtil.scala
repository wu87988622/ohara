package com.island.ohara.integration

import com.island.ohara.io.ByteUtil
import com.island.ohara.io.CloseOnce._
import com.typesafe.scalalogging.Logger
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, ByteArraySerializer}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class TestOharaTestUtil extends FlatSpec with Matchers {

  private[this] lazy val logger = Logger(getClass.getName)

  "creating 3 brokers" should "work" in {
    doClose(new OharaTestUtil(3)) {
      testUtil => {
        testUtil.kafkaBrokers.size shouldBe 3
        testUtil.createTopic("my_topic")
        testUtil.exist("my_topic") shouldBe true
        val (_, valueQueue) = testUtil.run("my_topic", new ByteArrayDeserializer, new ByteArrayDeserializer)
        val totalMessageCount = 100
        doClose(new KafkaProducer[Array[Byte], Array[Byte]](testUtil.properties, new ByteArraySerializer, new ByteArraySerializer)) {
          producer => {
            var count: Int = totalMessageCount
            while (count > 0) {
              producer.send(new ProducerRecord[Array[Byte], Array[Byte]]("my_topic", ByteUtil.toBytes("key"), ByteUtil.toBytes("value")))
              count -= 1
            }
          }
        }
        testUtil.await(() => valueQueue.size() == totalMessageCount, 1 minute)
        valueQueue.forEach((value: Array[Byte]) => ByteUtil.toString(value) shouldBe "value")
      }
    }
  }
}
