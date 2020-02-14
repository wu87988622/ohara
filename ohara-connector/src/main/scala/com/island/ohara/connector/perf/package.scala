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

package com.island.ohara.connector

import java.util.concurrent.atomic.AtomicInteger

import com.island.ohara.common.data.{Column, DataType}
import com.island.ohara.common.setting.SettingDef

import scala.concurrent.duration.Duration

package object perf {
  val PERF_BATCH_KEY: String     = "perf.batch"
  val PERF_FREQUENCY_KEY: String = "perf.frequency"

  val PERF_BATCH_DEFAULT: Int          = 10
  val PERF_FREQUENCY_DEFAULT: Duration = Duration("1 second")

  val PERF_CELL_LENGTH_KEY: String  = "perf.cell.length"
  val PERF_CELL_LENGTH_DEFAULT: Int = 10

  private[this] val GROUP_COUNT = new AtomicInteger()

  val DEFINITIONS = Map(
    PERF_BATCH_KEY -> SettingDef
      .builder()
      .displayName("Batch")
      .documentation("The batch of perf")
      .key(PERF_BATCH_KEY)
      .optional(PERF_BATCH_DEFAULT)
      .orderInGroup(GROUP_COUNT.getAndIncrement())
      .build(),
    PERF_FREQUENCY_KEY -> SettingDef
      .builder()
      .displayName("Frequency")
      .documentation("The frequency of perf")
      .key(PERF_FREQUENCY_KEY)
      .optional(java.time.Duration.ofMillis(PERF_FREQUENCY_DEFAULT.toMillis))
      .orderInGroup(GROUP_COUNT.getAndIncrement())
      .build(),
    PERF_CELL_LENGTH_KEY -> SettingDef
      .builder()
      .displayName("cell length")
      .documentation("increase this value if you prefer to large cell. Noted, it works only for string type")
      .key(PERF_CELL_LENGTH_KEY)
      .optional(PERF_CELL_LENGTH_DEFAULT)
      .orderInGroup(GROUP_COUNT.getAndIncrement())
      .build()
  )

  /**
    * this is the default schema used to generate random data in perf source.
    * Since schema is not "required" in ohara, making a default schema avoid confusing users when they miss the schema
    */
  val DEFAULT_SCHEMA: Seq[Column] = Seq(
    Column.builder().name("a").dataType(DataType.BYTES).order(0).build(),
    Column.builder().name("b").dataType(DataType.BYTES).order(1).build(),
    Column.builder().name("c").dataType(DataType.BYTES).order(2).build()
  )
}
