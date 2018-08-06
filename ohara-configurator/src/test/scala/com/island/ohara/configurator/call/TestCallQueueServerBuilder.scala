package com.island.ohara.configurator.call

import com.island.ohara.integration.With3Blockers
import org.apache.kafka.common.config.TopicConfig
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.duration._
class TestCallQueueServerBuilder extends With3Blockers with Matchers {

  @Test
  def testIncompleteArguments(): Unit = {
    var builder = CallQueueServer.builder
    an[NoSuchElementException] should be thrownBy builder.build()
    builder = builder.initializationTimeout(10 seconds)
    an[NoSuchElementException] should be thrownBy builder.build()
    builder = builder.pollTimeout(1 seconds)
    an[NoSuchElementException] should be thrownBy builder.build()
    builder = builder.numberOfReplications(1)
    an[NoSuchElementException] should be thrownBy builder.build()
    builder = builder.numberOfPartitions(1)
    an[NoSuchElementException] should be thrownBy builder.build()
    builder = builder.topicName(methodName)
    an[NoSuchElementException] should be thrownBy builder.build()
    builder = builder.brokers(testUtil.brokersString)
    an[NoSuchElementException] should be thrownBy builder.build()
    builder = builder.groupId("xxx")
    builder.build().close()
  }

  @Test
  def testInvalidTopicOptions(): Unit = {
    an[IllegalArgumentException] should be thrownBy CallQueueServer.builder
      .initializationTimeout(10 seconds)
      .pollTimeout(1 seconds)
      .numberOfReplications(1)
      .numberOfPartitions(1)
      .topicName(methodName)
      .brokers(testUtil.brokersString)
      .groupId("xxx")
      // the following option is invalid for topic store
      .topicOptions(Map(TopicConfig.CLEANUP_POLICY_CONFIG -> TopicConfig.CLEANUP_POLICY_COMPACT))
      .build()
  }
}
