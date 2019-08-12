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

package com.island.ohara.connector.console

import com.island.ohara.common.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers
import scala.collection.JavaConverters._
class TestConsoleSink extends SmallTest with Matchers {

  @Test
  def testFrequenceDefinitions(): Unit = {
    val sink = new ConsoleSink

    val freqDef = sink.definitions().asScala.find(_.key() == CONSOLE_FREQUENCE).get
    freqDef.documentation() shouldBe CONSOLE_FREQUENCE_DOC
    freqDef.defaultValue() shouldBe CONSOLE_FREQUENCE_DEFAULT.toString
  }

  @Test
  def testDividerDefinitions(): Unit = {
    val sink = new ConsoleSink

    val dividerDef = sink.definitions().asScala.find(_.key() == CONSOLE_ROW_DIVIDER).get
    dividerDef.documentation() shouldBe CONSOLE_ROW_DIVIDER_DOC
    dividerDef.defaultValue() shouldBe CONSOLE_ROW_DIVIDER_DEFAULT
  }
}
