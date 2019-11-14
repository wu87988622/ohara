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

package com.island.ohara.connector.jio

import com.island.ohara.common.rule.OharaTest
import com.island.ohara.kafka.connector.json.ConnectorDefUtils
import org.junit.Test
import org.scalatest.Matchers._

import scala.collection.JavaConverters._
class TestJioSettings extends OharaTest {

  @Test
  def testBufferSetting(): Unit = {
    DATA_BUFFER_SIZE_DEFINITION.key() shouldBe DATA_BUFFER_SIZE_KEY
    DATA_BUFFER_SIZE_DEFINITION.defaultInt shouldBe DATA_BUFFER_SIZE_DEFAULT
  }

  @Test
  def testCloseTimeoutSetting(): Unit = {
    CLOSE_TIMEOUT_DEFINITION.key() shouldBe CLOSE_TIMEOUT_KEY
    CLOSE_TIMEOUT_DEFINITION.defaultDuration() shouldBe java.time.Duration.ofMillis(CLOSE_TIMEOUT_DEFAULT.toMillis)
  }

  @Test
  def testBindingTimeoutSetting(): Unit = {
    BINDING_TIMEOUT_DEFINITION.key() shouldBe BINDING_TIMEOUT_KEY
    BINDING_TIMEOUT_DEFINITION.defaultDuration() shouldBe java.time.Duration.ofMillis(BINDING_TIMEOUT_DEFAULT.toMillis)
  }

  @Test
  def testBindingPortSetting(): Unit = {
    BINDING_PORT_DEFINITION.key() shouldBe BINDING_PORT_KEY
    BINDING_PORT_DEFINITION.hasDefault shouldBe false
  }

  @Test
  def testBindingPathSetting(): Unit = {
    BINDING_PATH_DEFINITION.key() shouldBe BINDING_PATH_KEY
    BINDING_PATH_DEFINITION.defaultString() shouldBe BINDING_PATH_DEFAULT
  }

  @Test
  def jsonInHaveNoColumns(): Unit =
    new JsonIn().settingDefinitions().asScala.find(_ == ConnectorDefUtils.COLUMNS_DEFINITION) shouldBe None

  @Test
  def jsonOutHaveNoColumns(): Unit =
    new JsonOut().settingDefinitions().asScala.find(_ == ConnectorDefUtils.COLUMNS_DEFINITION) shouldBe None
}
