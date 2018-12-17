package com.island.ohara.kafka
import com.island.ohara.common.util.ReleaseOnce
import com.island.ohara.integration.With3Brokers
import org.apache.kafka.common.config.TopicConfig
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.collection.JavaConverters._

class TestKafkaClient extends With3Brokers with Matchers {
  private[this] val timeout = java.time.Duration.ofSeconds(10)

  private[this] val client = KafkaClient.of(testUtil.brokersConnProps)

  @Test
  def testAddPartitions(): Unit = {
    val topicName = methodName
    client.topicCreator().numberOfPartitions(1).numberOfReplications(1).create(topicName)
    client.topicDescription(topicName).numberOfPartitions shouldBe 1

    client.addPartitions(topicName, 2)
    client.topicDescription(topicName).numberOfPartitions shouldBe 2

    // decrease the number
    an[IllegalArgumentException] should be thrownBy client.addPartitions(topicName, 1)
    // alter an nonexistent topic
    an[IllegalArgumentException] should be thrownBy client.addPartitions("Xxx", 2, timeout)
  }

  @Test
  def testCreate(): Unit = {
    val topicName = methodName
    val numberOfPartitions = 2
    val numberOfReplications = 2.toShort
    client
      .topicCreator()
      .numberOfPartitions(numberOfPartitions)
      .numberOfReplications(numberOfReplications)
      .create(topicName)

    val topicInfo = client.topicDescription(topicName)
    topicInfo.name shouldBe topicName
    topicInfo.numberOfPartitions shouldBe numberOfPartitions
    topicInfo.numberOfPartitions shouldBe numberOfReplications

    client.deleteTopic(topicName)
    client.exist(topicName) shouldBe false
  }

  @Test
  def testTopicOptions(): Unit = {
    val topicName = methodName
    val numberOfPartitions = 2
    val numberOfReplications = 2.toShort
    val options = Map(
      TopicConfig.CLEANUP_POLICY_CONFIG -> TopicConfig.CLEANUP_POLICY_DELETE
    )
    client
      .topicCreator()
      .numberOfPartitions(numberOfPartitions)
      .numberOfReplications(numberOfReplications)
      .options(options.asJava)
      .create(topicName)

    val desc = client.topicDescription(topicName)
    desc.name shouldBe topicName
    desc.numberOfPartitions shouldBe numberOfPartitions
    desc.numberOfPartitions shouldBe numberOfReplications
    desc.options.asScala
      .filter(_.key == TopicConfig.CLEANUP_POLICY_CONFIG)
      .head
      .value shouldBe TopicConfig.CLEANUP_POLICY_DELETE
  }

  @After
  def cleanup(): Unit = {
    ReleaseOnce.close(client)
  }
}
