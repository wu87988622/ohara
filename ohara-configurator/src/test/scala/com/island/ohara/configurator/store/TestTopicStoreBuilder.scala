package com.island.ohara.configurator.store

import com.island.ohara.integration.OharaTestUtil
import com.island.ohara.rule.MediumTest
import com.island.ohara.serialization.StringSerializer
import org.apache.kafka.common.config.TopicConfig
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.duration._

class TestTopicStoreBuilder extends MediumTest with Matchers {

  private[this] val testUtil = new OharaTestUtil(3, 3)

  @Test
  def testIncompleteArguments(): Unit = {
    var builder = Store.builder(StringSerializer, StringSerializer)
    an[NoSuchElementException] should be thrownBy builder.build()
    builder = builder.initializationTimeout(10 seconds)
    an[NoSuchElementException] should be thrownBy builder.build()
    builder = builder.pollTimeout(1 seconds)
    an[NoSuchElementException] should be thrownBy builder.build()
    builder = builder.replications(1)
    an[NoSuchElementException] should be thrownBy builder.build()
    builder = builder.partitions(3)
    an[NoSuchElementException] should be thrownBy builder.build()
    builder = builder.topicName(testName.getMethodName)
    an[NoSuchElementException] should be thrownBy builder.build()
    builder = builder.brokers(testUtil.brokersString)
    builder.build().close()
  }

  @Test
  def testInvalidTopicOptions(): Unit = {
    an[IllegalArgumentException] should be thrownBy Store
      .builder(StringSerializer, StringSerializer)
      .initializationTimeout(10 seconds)
      .pollTimeout(1 seconds)
      .replications(1)
      .partitions(3)
      .topicName(testName.getMethodName)
      .brokers(testUtil.brokersString)
      .topicOptions(Map(TopicConfig.CLEANUP_POLICY_CONFIG -> TopicConfig.CLEANUP_POLICY_DELETE))
      .build()
  }
}
