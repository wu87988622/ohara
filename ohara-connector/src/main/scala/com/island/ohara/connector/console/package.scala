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

import com.island.ohara.common.setting.SettingDef

import scala.concurrent.duration._

package object console {
  /**
    * used to set the order of definitions.
    */
  private[this] val COUNTER               = new AtomicInteger(0)
  val CONSOLE_FREQUENCE: String           = "console.sink.frequence"
  val CONSOLE_FREQUENCE_DOC: String       = "the frequence to print the row on log"
  val CONSOLE_FREQUENCE_DEFAULT: Duration = 3 seconds
  val CONSOLE_FREQUENCE_DEFINITION: SettingDef = SettingDef
    .builder()
    .displayName("the frequence of printing data")
    .key(CONSOLE_FREQUENCE)
    .documentation(CONSOLE_FREQUENCE_DOC)
    .optional(java.time.Duration.ofMillis(CONSOLE_FREQUENCE_DEFAULT.toMillis))
    .orderInGroup(COUNTER.getAndIncrement())
    .build()

  val CONSOLE_ROW_DIVIDER: String = "console.sink.row.divider"
  val CONSOLE_ROW_DIVIDER_DOC: String =
    "the charset used to divide the rows. For example, the divider \"|\" makes the output string: row_0|row_1"
  val CONSOLE_ROW_DIVIDER_DEFAULT: String = "|"
  val CONSOLE_ROW_DIVIDER_DEFINITION: SettingDef = SettingDef
    .builder()
    .displayName("the divider charset to distinguish each row")
    .key(CONSOLE_ROW_DIVIDER)
    .documentation(CONSOLE_ROW_DIVIDER_DOC)
    .optional(CONSOLE_ROW_DIVIDER_DEFAULT)
    .orderInGroup(COUNTER.getAndIncrement())
    .build()
}
