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

package com.island.ohara.it
import com.island.ohara.common.setting.SettingDef
import com.island.ohara.common.setting.SettingDef.Type

package object connector {

  /**
    * add some definitions for testing.
    */
  val DUMB_SETTING_DEFINITIONS: Seq[SettingDef] = Seq(
    SettingDef
      .builder()
      .displayName("dumb boolean")
      .key("dumb.boolean")
      .documentation("boolean for testing")
      .optional(false)
      .build(),
    SettingDef
      .builder()
      .displayName("dumb short")
      .key("dumb.short")
      .documentation("short for testing")
      .optional(10.asInstanceOf[Short])
      .build(),
    SettingDef
      .builder()
      .displayName("dumb integer")
      .key("dumb.integer")
      .documentation("integer for testing")
      .optional(10)
      .build(),
    SettingDef
      .builder()
      .displayName("dumb long")
      .key("dumb.long")
      .documentation("long for testing")
      .optional(10L)
      .build(),
    SettingDef
      .builder()
      .displayName("dumb double")
      .key("dumb.double")
      .documentation("double for testing")
      .optional(10D)
      .build(),
    SettingDef
      .builder()
      .displayName("dumb array")
      .key("dumb.array")
      .optional(Type.ARRAY)
      .documentation("array for testing")
      .build(),
    SettingDef
      .builder()
      .displayName("dumb password")
      .key("dumb.password")
      .optional(Type.PASSWORD)
      .documentation("password for testing")
      .build(),
    SettingDef
      .builder()
      .displayName("dumb jdbc table")
      .key("dumb.jdbc.table")
      .optional(Type.JDBC_TABLE)
      .documentation("jdbc table for testing")
      .build(),
    SettingDef
      .builder()
      .displayName("dumb duration")
      .key("dumb.duration")
      .documentation("duration for testing")
      .optional(java.time.Duration.ofSeconds(30))
      .build(),
    SettingDef
      .builder()
      .displayName("dumb port")
      .key("dumb.port")
      .documentation("port for testing")
      .optional("9999")
      .build(),
    SettingDef.builder().displayName("dumb tags").key("dumb.tags").optional(Type.TAGS).documentation("Tags").build(),
  )
}
