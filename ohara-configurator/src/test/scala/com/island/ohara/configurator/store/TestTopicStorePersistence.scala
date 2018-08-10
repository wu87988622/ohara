package com.island.ohara.configurator.store

import com.island.ohara.config.UuidUtil
import com.island.ohara.integration.{OharaTestUtil, With3Brokers}
import com.island.ohara.io.CloseOnce._
import com.island.ohara.kafka.Consumer
import com.island.ohara.serialization.{Serializer, StringSerializer}
import org.apache.kafka.common.config.TopicConfig
import org.junit.Test
import org.scalatest.Matchers

import scala.collection.mutable.ArrayBuffer

class TestTopicStorePersistence extends With3Brokers with Matchers {

  @Test
  def testRetention(): Unit = {
    val specifiedKey = "specifiedKey"
    val topicName = methodName
    val numberOfOtherMessages = 2048
    doClose(
      Store
        .builder(StringSerializer, StringSerializer)
        .brokers(testUtil.brokersString)
        .topicName(topicName)
        // make small retention so as to trigger log clear
        .topicOptions(
          Map(TopicConfig.FILE_DELETE_DELAY_MS_CONFIG -> "1000", TopicConfig.SEGMENT_BYTES_CONFIG -> "1024"))
        .build()) { store =>
      {
        0 until 10 foreach (index => store.update(specifiedKey, index.toString))
        // the local cache do the de-duplicate
        store.size shouldBe 1
        store.iterator.next()._2 shouldBe 9.toString

        0 until numberOfOtherMessages foreach (index => store.update(index.toString, index.toString))
        store.size shouldBe (numberOfOtherMessages + 1)
      }
    }
    import scala.concurrent.duration._
    def verifyTopicContent(timeout: Duration): Boolean = doClose(
      Consumer
        .builder(Serializer.STRING, Serializer.STRING)
        .brokers(testUtil.brokersString)
        .fromBegin(true)
        .groupId(UuidUtil.uuid())
        .topicName(topicName)
        .build()) { consumer =>
      {
        val messageBuffer = new ArrayBuffer[String]()
        def checkBuffer(): Boolean = messageBuffer.filter(_.equals(specifiedKey)).size == 1 && messageBuffer
          .filterNot(_.equals(specifiedKey))
          .size == numberOfOtherMessages
        val endtime = System.currentTimeMillis() + timeout.toMillis
        while (!checkBuffer() && System.currentTimeMillis() < endtime)(consumer
          .poll(5 seconds)
          .foreach(_.key.foreach(messageBuffer += _)))
        checkBuffer()
      }
    }
    OharaTestUtil.await(() => verifyTopicContent(10 seconds), 30 seconds)
  }
}
