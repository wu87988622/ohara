package com.island.ohara.kafka
import com.island.ohara.integration.With3Brokers
import org.apache.kafka.common.config.TopicConfig
import org.junit.Test
import org.scalatest.Matchers

class TestKafkaUtil extends With3Brokers with Matchers {

  @Test
  def testAddPartitions(): Unit = {
    val topicName = methodName
    KafkaUtil.createTopic(testUtil.brokersConnProps, topicName, 1, 1)
    KafkaUtil.topicDescription(testUtil.brokersConnProps, topicName).numberOfPartitions shouldBe 1

    KafkaUtil.addPartitions(testUtil.brokersConnProps, topicName, 2)
    KafkaUtil.topicDescription(testUtil.brokersConnProps, topicName).numberOfPartitions shouldBe 2

    // decrease the number
    an[IllegalArgumentException] should be thrownBy KafkaUtil.addPartitions(testUtil.brokersConnProps, topicName, 1)
    // alter an nonexistent topic
    an[IllegalArgumentException] should be thrownBy KafkaUtil.addPartitions(testUtil.brokersConnProps, "Xxx", 2)
  }

  @Test
  def testCreate(): Unit = {
    val topicName = methodName
    val numberOfPartitions = 2
    val numberOfReplications = 2.toShort
    KafkaUtil.createTopic(testUtil.brokersConnProps, topicName, numberOfPartitions, numberOfReplications)

    val topicInfo = KafkaUtil.topicDescription(testUtil.brokersConnProps, topicName)
    topicInfo.name shouldBe topicName
    topicInfo.numberOfPartitions shouldBe numberOfPartitions
    topicInfo.numberOfReplications shouldBe numberOfReplications

    KafkaUtil.deleteTopic(testUtil.brokersConnProps, topicName)
    KafkaUtil.exist(testUtil.brokersConnProps, topicName) shouldBe false
  }

  @Test
  def testTopicOptions(): Unit = {
    val topicName = methodName
    val numberOfPartitions = 2
    val numberOfReplications = 2.toShort
    val options = Map(
      TopicConfig.CLEANUP_POLICY_CONFIG -> TopicConfig.CLEANUP_POLICY_DELETE
    )
    KafkaUtil.createTopic(testUtil.brokersConnProps, topicName, numberOfPartitions, numberOfReplications, options)

    val topicInfo = KafkaUtil.topicDescription(testUtil.brokersConnProps, topicName)
    topicInfo.name shouldBe topicName
    topicInfo.numberOfPartitions shouldBe numberOfPartitions
    topicInfo.numberOfReplications shouldBe numberOfReplications
    topicInfo.options
      .filter(_.key == TopicConfig.CLEANUP_POLICY_CONFIG)
      .head
      .value shouldBe TopicConfig.CLEANUP_POLICY_DELETE
  }

}
