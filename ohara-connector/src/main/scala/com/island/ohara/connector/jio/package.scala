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

import com.island.ohara.common.data.Row
import com.island.ohara.common.setting.SettingDef
import spray.json.JsObject

import scala.concurrent.duration._
package object jio {
  /**
    * used to set the order of definitions.
    */
  private[this] val COUNTER = new AtomicInteger(0)

  val DATA_BUFFER_SIZE_KEY: String  = "jio.data.buffer.size"
  val DATA_BUFFER_SIZE_DEFAULT: Int = 100
  val DATA_BUFFER_SIZE_DEFINITION: SettingDef = SettingDef
    .builder()
    .key(DATA_BUFFER_SIZE_KEY)
    .documentation("the maximum number of buffer data")
    .optional(DATA_BUFFER_SIZE_DEFAULT)
    .orderInGroup(COUNTER.getAndIncrement())
    .build()
  val CLOSE_TIMEOUT_KEY: String             = "jio.close.timeout"
  val CLOSE_TIMEOUT_DEFAULT: FiniteDuration = 30 seconds
  val CLOSE_TIMEOUT_DEFINITION: SettingDef = SettingDef
    .builder()
    .key(CLOSE_TIMEOUT_KEY)
    .documentation("the time to wait server to close all inner objects")
    .optional(java.time.Duration.ofMillis(CLOSE_TIMEOUT_DEFAULT.toMillis))
    .orderInGroup(COUNTER.getAndIncrement())
    .build()
  val BINDING_TIMEOUT_KEY: String             = "jio.binding.timeout"
  val BINDING_TIMEOUT_DEFAULT: FiniteDuration = 30 seconds
  val BINDING_TIMEOUT_DEFINITION: SettingDef = SettingDef
    .builder()
    .key(BINDING_TIMEOUT_KEY)
    .documentation("the time to wait server to bind on specific port")
    .optional(java.time.Duration.ofMillis(BINDING_TIMEOUT_DEFAULT.toMillis))
    .orderInGroup(COUNTER.getAndIncrement())
    .build()
  val BINDING_PORT_KEY: String = "jio.binding.port"
  val BINDING_PORT_DEFINITION: SettingDef = SettingDef
    .builder()
    .key(BINDING_PORT_KEY)
    .documentation("the port used to set up the restful server")
    .required(SettingDef.Type.BINDING_PORT)
    .orderInGroup(COUNTER.getAndIncrement())
    .build()
  val BINDING_PATH_KEY: String     = "jio.binding.path"
  val BINDING_PATH_DEFAULT: String = "data"
  val BINDING_PATH_DEFINITION: SettingDef = SettingDef
    .builder()
    .key(BINDING_PATH_KEY)
    .documentation(
      "the path to url resource. For example, if you define the path=abc and port=11111, the url bound by JI connector is host:11111/abc"
    )
    .optional(BINDING_PATH_DEFAULT)
    .orderInGroup(COUNTER.getAndIncrement())
    .build()

  /**
    * the core settings for JsonIn.
    */
  val DEFINITIONS: Map[String, SettingDef] = Seq(
    DATA_BUFFER_SIZE_DEFINITION,
    CLOSE_TIMEOUT_DEFINITION,
    BINDING_TIMEOUT_DEFINITION,
    BINDING_PORT_DEFINITION,
    BINDING_PATH_DEFINITION
  ).map(d => d.key() -> d).toMap

  def toJson(row: Row): JsObject = com.island.ohara.client.configurator.v0.toJson(row)

  def toRow(obj: JsObject): Row = com.island.ohara.client.configurator.v0.toRow(obj)
}
