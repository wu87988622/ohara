package com.island.ohara.configurator.store

import com.island.ohara.integration.OharaTestUtil
import com.island.ohara.rule.MediumTest
import com.island.ohara.serialization.StringSerializer
import org.apache.kafka.common.config.TopicConfig
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.duration._

class TestTopicStoreBuilder extends MediumTest with Matchers {

  private[this] val testUtil = OharaTestUtil.localBrokers(1)

  @Test
  def testIncompleteArguments(): Unit = {
    var builder = Store.builder(StringSerializer, StringSerializer)
    an[NoSuchElementException] should be thrownBy builder.build()
    builder = builder.initializationTimeout(10 seconds)
    an[NoSuchElementException] should be thrownBy builder.build()
    builder = builder.pollTimeout(1 seconds)
    an[NoSuchElementException] should be thrownBy builder.build()
    builder = builder.numberOfReplications(1)
    an[NoSuchElementException] should be thrownBy builder.build()
    builder = builder.numberOfPartitions(3)
    an[NoSuchElementException] should be thrownBy builder.build()
    builder = builder.topicName(methodName)
    an[NoSuchElementException] should be thrownBy builder.build()
    builder = builder.brokers(testUtil.brokers)
    builder.build().close()
  }

  @Test
  def testInvalidTopicOptions(): Unit = {
    an[IllegalArgumentException] should be thrownBy Store
      .builder(StringSerializer, StringSerializer)
      .initializationTimeout(10 seconds)
      .pollTimeout(1 seconds)
      .numberOfReplications(1)
      .numberOfPartitions(1)
      .topicName(methodName)
      .brokers(testUtil.brokers)
      // the following option is invalid for topic store
      .topicOptions(Map(TopicConfig.CLEANUP_POLICY_CONFIG -> TopicConfig.CLEANUP_POLICY_DELETE))
      .build()
  }

  @After
  def tearDown(): Unit = {
    testUtil.close()
  }
}
