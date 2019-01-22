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

package com.island.ohara.configurator

import com.island.ohara.client.configurator.v0.ConnectorApi
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.Releasable
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.Await
import scala.concurrent.duration._
class TestOhara699 extends SmallTest with Matchers {

  private[this] val configurator = Configurator.builder().fake().build()

  private[this] val access = ConnectorApi.access().hostname(configurator.hostname).port(configurator.port)

  @Test
  def testStartAnNonexistentConnector(): Unit = {
    an[IllegalArgumentException] should be thrownBy Await.result(access.start(methodName()), 30 seconds)
  }

  @Test
  def testStopAnNonexistentConnector(): Unit = {
    an[IllegalArgumentException] should be thrownBy Await.result(access.stop(methodName()), 30 seconds)
  }
  @Test
  def testPauseAnNonexistentConnector(): Unit = {
    an[IllegalArgumentException] should be thrownBy Await.result(access.pause(methodName()), 30 seconds)
  }

  @Test
  def testResumeAnNonexistentConnector(): Unit = {
    an[IllegalArgumentException] should be thrownBy Await.result(access.resume(methodName()), 30 seconds)
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
