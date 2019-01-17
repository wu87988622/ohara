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

import com.island.ohara.integration.With3Brokers
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.duration._
class TestCallQueueServerBuilder extends With3Brokers with Matchers {

  @Test
  def testIncompleteArguments(): Unit = {
    var builder = CallQueueServer.builder()
    an[NoSuchElementException] should be thrownBy builder.build()
    builder = builder.pollTimeout(1 seconds)
    an[NoSuchElementException] should be thrownBy builder.build()
    builder = builder.responseTopic(methodName)
    an[NoSuchElementException] should be thrownBy builder.build()
    builder = builder.requestTopic(methodName)
    an[NoSuchElementException] should be thrownBy builder.build()
    builder = builder.brokers(testUtil.brokersConnProps)
    an[NoSuchElementException] should be thrownBy builder.build()
    builder = builder.groupId("xxx")
    builder.build().close()
  }
}
