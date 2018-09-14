package com.island.ohara.configurator.store

import com.island.ohara.integration.OharaTestUtil
import com.island.ohara.kafka.KafkaUtil
import com.island.ohara.rule.MediumTest
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.duration._

class TestTopicStoreBuilder extends MediumTest with Matchers {

  private[this] val testUtil = OharaTestUtil.localBrokers(1)

  @Test
  def testIncompleteArguments(): Unit = {
    val topicName = methodName
    KafkaUtil.createTopic(testUtil.brokers, topicName, 1, 1)
    var builder = Store.builder()
    an[NoSuchElementException] should be thrownBy builder.build[String, String]
    builder = builder.pollTimeout(1 seconds)
    an[NoSuchElementException] should be thrownBy builder.build[String, String]
    builder = builder.topicName(topicName)
    an[NoSuchElementException] should be thrownBy builder.build[String, String]
    builder = builder.brokers(testUtil.brokers)
    builder.build[String, String].close()
  }

  @After
  def tearDown(): Unit = {
    testUtil.close()
  }
}
