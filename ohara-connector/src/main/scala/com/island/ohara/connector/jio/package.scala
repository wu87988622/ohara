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

import com.island.ohara.common.setting.SettingDef
import scala.concurrent.duration._
package object jio {

  val DATA_BUFFER_SIZE_KEY: String = "jio.data.buffer.size"
  val DATA_BUFFER_SIZE_DEFAULT: Int = 100
  val DATA_BUFFER_SIZE_DEFINITION: SettingDef = SettingDef
    .builder()
    .key(DATA_BUFFER_SIZE_KEY)
    .documentation("the maximum number of buffer data")
    .valueType(SettingDef.Type.INT)
    .optional(DATA_BUFFER_SIZE_DEFAULT)
    .build()
  val CLOSE_TIMEOUT_KEY: String = "jio.close.timeout"
  val CLOSE_TIMEOUT_DEFAULT: FiniteDuration = 30 seconds
  val CLOSE_TIMEOUT_DEFINITION: SettingDef = SettingDef
    .builder()
    .key(CLOSE_TIMEOUT_KEY)
    .documentation("the time to wait server to close all inner objects")
    .valueType(SettingDef.Type.DURATION)
    .optional(java.time.Duration.ofMillis(CLOSE_TIMEOUT_DEFAULT.toMillis))
    .build()
  val BINDING_TIMEOUT_KEY: String = "jio.binding.timeout"
  val BINDING_TIMEOUT_DEFAULT: FiniteDuration = 30 seconds
  val BINDING_TIMEOUT_DEFINITION: SettingDef = SettingDef
    .builder()
    .key(BINDING_TIMEOUT_KEY)
    .documentation("the time to wait server to bind on specific port")
    .valueType(SettingDef.Type.DURATION)
    .optional(java.time.Duration.ofMillis(BINDING_TIMEOUT_DEFAULT.toMillis))
    .build()
  val BINDING_PORT_KEY: String = "jio.binding.port"
  val BINDING_PORT_DEFINITION: SettingDef = SettingDef
    .builder()
    .key(BINDING_PORT_KEY)
    .documentation("the port used to set up the restful server")
    .valueType(SettingDef.Type.BINDING_PORT)
    .build()
  val BINDING_PATH_KEY: String = "jio.binding.path"
  val BINDING_PATH_DEFAULT: String = "data"
  val BINDING_PATH_DEFINITION: SettingDef = SettingDef
    .builder()
    .key(BINDING_PATH_KEY)
    .documentation(
      "the path to url resource. For example, if you define the path=abc and port=11111, the url bound by JI connector is host:11111/abc")
    .valueType(SettingDef.Type.STRING)
    .optional(BINDING_PATH_DEFAULT)
    .build()

  /**
    * the core settings for JsonIn.
    */
  val DEFINITIONS: Seq[SettingDef] = Seq(
    DATA_BUFFER_SIZE_DEFINITION,
    CLOSE_TIMEOUT_DEFINITION,
    BINDING_TIMEOUT_DEFINITION,
    BINDING_PORT_DEFINITION,
    BINDING_PATH_DEFINITION
  )
}
