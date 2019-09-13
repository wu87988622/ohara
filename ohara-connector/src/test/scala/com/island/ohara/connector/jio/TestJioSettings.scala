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

import com.island.ohara.common.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

class TestJioSettings extends SmallTest with Matchers {

  @Test
  def testBufferSetting(): Unit = {
    DATA_BUFFER_SIZE_DEFINITION.key() shouldBe DATA_BUFFER_SIZE_KEY
    DATA_BUFFER_SIZE_DEFINITION.defaultValue() shouldBe DATA_BUFFER_SIZE_DEFAULT.toString
  }

  @Test
  def testCloseTimeoutSetting(): Unit = {
    CLOSE_TIMEOUT_DEFINITION.key() shouldBe CLOSE_TIMEOUT_KEY
    CLOSE_TIMEOUT_DEFINITION
      .defaultValue() shouldBe java.time.Duration.ofMillis(CLOSE_TIMEOUT_DEFAULT.toMillis).toString
  }

  @Test
  def testBindingTimeoutSetting(): Unit = {
    BINDING_TIMEOUT_DEFINITION.key() shouldBe BINDING_TIMEOUT_KEY
    BINDING_TIMEOUT_DEFINITION
      .defaultValue() shouldBe java.time.Duration.ofMillis(BINDING_TIMEOUT_DEFAULT.toMillis).toString
  }

  @Test
  def testBindingPortSetting(): Unit = {
    BINDING_PORT_DEFINITION.key() shouldBe BINDING_PORT_KEY
    BINDING_PORT_DEFINITION.defaultValue() shouldBe null
  }

  @Test
  def testBindingPathSetting(): Unit = {
    BINDING_PATH_DEFINITION.key() shouldBe BINDING_PATH_KEY
    BINDING_PATH_DEFINITION.defaultValue() shouldBe BINDING_PATH_DEFAULT
  }
}
