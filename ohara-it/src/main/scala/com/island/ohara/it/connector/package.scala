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

import com.island.ohara.kafka.connector.json.SettingDefinition
import com.island.ohara.kafka.connector.json.SettingDefinition.Type

package object connector {

  /**
    * add some definitions for testing.
    */
  val DUMB_SETTING_DEFINITIONS: Seq[SettingDefinition] = Seq(
    SettingDefinition
      .builder()
      .displayName("dumb boolean")
      .key("dumb.boolean")
      .valueType(Type.BOOLEAN)
      .documentation("boolean for testing")
      .optional("false")
      .build(),
    SettingDefinition
      .builder()
      .displayName("dumb boolean")
      .key("dumb.boolean")
      .valueType(Type.BOOLEAN)
      .documentation("boolean for testing")
      .optional("false")
      .build(),
    SettingDefinition
      .builder()
      .displayName("dumb short")
      .key("dumb.short")
      .valueType(Type.SHORT)
      .documentation("short for testing")
      .optional("10")
      .build(),
    SettingDefinition
      .builder()
      .displayName("dumb integer")
      .key("dumb.integer")
      .valueType(Type.INT)
      .documentation("integer for testing")
      .optional("10")
      .build(),
    SettingDefinition
      .builder()
      .displayName("dumb long")
      .key("dumb.long")
      .valueType(Type.LONG)
      .documentation("long for testing")
      .optional("10")
      .build(),
    SettingDefinition
      .builder()
      .displayName("dumb double")
      .key("dumb.double")
      .valueType(Type.DOUBLE)
      .documentation("double for testing")
      .optional("10")
      .build(),
    SettingDefinition
      .builder()
      .displayName("dumb array")
      .key("dumb.array")
      .valueType(Type.ARRAY)
      .documentation("array for testing")
      .optional()
      .build(),
    SettingDefinition
      .builder()
      .displayName("dumb password")
      .key("dumb.password")
      .valueType(Type.PASSWORD)
      .documentation("password for testing")
      .optional()
      .build(),
    SettingDefinition
      .builder()
      .displayName("dumb jdbc table")
      .key("dumb.jdbc.table")
      .valueType(Type.JDBC_TABLE)
      .documentation("jdbc table for testing")
      .optional()
      .build(),
    SettingDefinition
      .builder()
      .displayName("dumb duration")
      .key("dumb.duration")
      .valueType(Type.DURATION)
      .documentation("duration for testing")
      .optional(java.time.Duration.ofSeconds(30).toString)
      .build(),
    SettingDefinition
      .builder()
      .displayName("dumb port")
      .key("dumb.port")
      .valueType(Type.PORT)
      .documentation("port for testing")
      .optional("9999")
      .build(),
  )
}
