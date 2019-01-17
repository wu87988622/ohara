/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.island.ohara.configurator.call

import com.island.ohara.common.rule.MediumTest
import com.island.ohara.integration.OharaTestUtil
import com.island.ohara.kafka.KafkaUtil
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.duration._

class TestCallQueueClientBuilder extends MediumTest with Matchers {
  private[this] val testUtil = OharaTestUtil.broker()

  @Test
  def testIncompleteArguments(): Unit = {
    var builder = CallQueueClient.builder()
    an[NoSuchElementException] should be thrownBy builder.build()
    builder = builder.pollTimeout(1 seconds)
    an[NoSuchElementException] should be thrownBy builder.build()
    builder = builder.requestTopic(methodName)
    an[NoSuchElementException] should be thrownBy builder.build()
    builder = builder.responseTopic(methodName)
    an[NoSuchElementException] should be thrownBy builder.build()
    builder = builder.brokers(testUtil.brokersConnProps)
    // we haven't created the topic
    an[IllegalArgumentException] should be thrownBy builder.build()
    KafkaUtil.createTopic(testUtil.brokersConnProps, methodName, 1, 1)
    builder.build().close()
  }

  @After
  def tearDown(): Unit = {
    testUtil.close()
  }
}
