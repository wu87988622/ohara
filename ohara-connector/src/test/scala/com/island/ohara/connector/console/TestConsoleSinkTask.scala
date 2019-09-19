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

import java.util.concurrent.TimeUnit

import com.island.ohara.common.data.Row
import com.island.ohara.common.rule.OharaTest
import com.island.ohara.common.util.CommonUtils
import org.apache.kafka.connect.sink.SinkRecord
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.duration._
import scala.collection.JavaConverters._
class TestConsoleSinkTask extends OharaTest with Matchers {

  private[this] def configs(key: String, value: String): java.util.Map[String, String] = Map(
    "name" -> CommonUtils.randomString(),
    key -> value
  ).asJava

  @Test
  def testEmptySetting(): Unit = {
    val task = new ConsoleSinkTask()
    task.start(java.util.Collections.singletonMap("name", CommonUtils.randomString()))
    task.freq shouldBe CONSOLE_FREQUENCE_DEFAULT
    task.divider shouldBe CONSOLE_ROW_DIVIDER_DEFAULT
  }

  @Test
  def testFrequence(): Unit = {
    val task = new ConsoleSinkTask()
    task.start(configs(CONSOLE_FREQUENCE, "10 seconds"))
    task.freq shouldBe (10 seconds)
  }

  @Test
  def testDivider(): Unit = {
    val task = new ConsoleSinkTask()
    val divider = CommonUtils.randomString()
    task.start(configs(CONSOLE_ROW_DIVIDER, divider))
    task.divider shouldBe divider
  }

  @Test
  def testPrint(): Unit = {
    val task = new ConsoleSinkTask()
    task.start(configs(CONSOLE_FREQUENCE, "2 seconds"))
    task.lastLog shouldBe -1

    task.put(java.util.Collections.emptyList())
    task.lastLog shouldBe -1

    putRecord(task)
    val lastLogCopy1 = task.lastLog
    lastLogCopy1 should not be -1

    TimeUnit.SECONDS.sleep(1)

    putRecord(task)
    val lastLogCopy2 = task.lastLog
    lastLogCopy2 shouldBe lastLogCopy1

    TimeUnit.SECONDS.sleep(1)

    putRecord(task)
    val lastLogCopy3 = task.lastLog
    lastLogCopy3 should not be lastLogCopy2
    lastLogCopy3 should not be -1
  }

  private[this] def putRecord(task: ConsoleSinkTask): Unit = task.put(
    java.util.Collections.singletonList(
      new SinkRecord(
        CommonUtils.randomString(),
        1,
        null,
        Row.EMPTY,
        null,
        null,
        1
      )))
}
