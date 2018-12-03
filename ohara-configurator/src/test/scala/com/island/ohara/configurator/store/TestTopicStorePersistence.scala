package com.island.ohara.configurator.store

import com.island.ohara.common.data.Serializer
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.integration.With3Brokers
import com.island.ohara.kafka.{Consumer, KafkaClient}
import org.apache.kafka.common.config.TopicConfig
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.{Await, Awaitable}
import scala.concurrent.duration._
class TestTopicStorePersistence extends With3Brokers with Matchers {

  private[this] def result[T](awaitable: Awaitable[T]): T = Await.result(awaitable, 30 seconds)
  @Test
  def testRetention(): Unit = {
    val specifiedKey = "specifiedKey"
    val topicName = methodName
    val numberOfOtherMessages = 2048
    val client = KafkaClient(testUtil.brokersConnProps)
    try client
      .topicCreator()
      .numberOfReplications(1)
      .numberOfPartitions(1)
      // make small retention so as to trigger log clear
      .options(Map(TopicConfig.FILE_DELETE_DELAY_MS_CONFIG -> "1000", TopicConfig.SEGMENT_BYTES_CONFIG -> "1024"))
      .compacted()
      .create(topicName)
    finally client.close()

    val store = Store
      .builder()
      .brokers(testUtil.brokersConnProps)
      .topicName(topicName)
      .build(Serializer.STRING, Serializer.STRING)
    try {
      0 until 10 foreach (index => result(store.update(specifiedKey, index.toString, Consistency.STRICT)))
      // the local cache do the de-duplicate
      store.size shouldBe 1
      store.iterator.next()._2 shouldBe 9.toString

      0 until numberOfOtherMessages foreach (index =>
        result(store.update(index.toString, index.toString, Consistency.STRICT)))
      store.size shouldBe (numberOfOtherMessages + 1)
    } finally store.close()

    import scala.concurrent.duration._
    def verifyTopicContent(timeout: scala.concurrent.duration.Duration): Boolean = {
      val consumer = Consumer
        .builder()
        .brokers(testUtil.brokersConnProps)
        .offsetFromBegin()
        .groupId(CommonUtil.uuid())
        .topicName(topicName)
        .build(Serializer.STRING, Serializer.STRING)

      try {
        val keys = consumer.poll(timeout, numberOfOtherMessages + 1).map(_.key.get)
        keys.count(_ == specifiedKey) == 1 && keys.count(_ != specifiedKey) == numberOfOtherMessages
      } finally consumer.close()
    }
    CommonUtil.await(() => verifyTopicContent(10 seconds), java.time.Duration.ofSeconds(20))
  }
}
