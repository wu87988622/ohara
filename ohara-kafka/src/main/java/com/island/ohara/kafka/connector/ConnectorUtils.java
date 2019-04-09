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

package com.island.ohara.kafka.connector;

import com.island.ohara.common.annotations.VisibleForTesting;
import com.island.ohara.common.data.Row;
import com.island.ohara.common.data.Serializer;
import com.island.ohara.common.util.ByteUtils;
import com.island.ohara.common.util.CommonUtils;
import com.island.ohara.kafka.connector.json.SettingDefinition;
import com.island.ohara.metrics.basic.Counter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.ConnectRecord;

final class ConnectorUtils {
  private static SettingDefinition copy(String value, SettingDefinition definition) {
    return SettingDefinition.builder(definition).optional(value).build();
  }

  static List<SettingDefinition> toSettingDefinitions(
      List<SettingDefinition> settingDefinitions, ConnectorVersion version) {
    return Stream.of(
            settingDefinitions,
            Arrays.asList(
                copy(version.version(), SettingDefinition.VERSION_DEFINITION),
                copy(version.revision(), SettingDefinition.REVISION_DEFINITION),
                copy(version.author(), SettingDefinition.AUTHOR_DEFINITION)))
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  static ConfigDef toConfigDef(List<SettingDefinition> settingDefinitions) {
    ConfigDef def = new ConfigDef();
    settingDefinitions.stream().map(SettingDefinition::toConfigKey).forEach(def::define);
    return def;
  }

  /**
   * Create and register a row counter with specific group name.
   *
   * @param group group name. It is normally equal to connector name
   * @return row counter
   */
  static Counter rowCounter(String group) {
    return Counter.builder()
        .group(group)
        .name("row.counter")
        .unit("rows")
        .document("number of rows")
        .startTime(CommonUtils.current())
        .value(0)
        .register();
  }

  /**
   * Create and register a size counter with specific group name.
   *
   * @param group group name. It is normally equal to connector name
   * @return size counter
   */
  static Counter sizeCounter(String group) {
    return Counter.builder()
        .group(group)
        .name("row.size")
        .unit("bytes")
        .document("size (in bytes) of rows")
        .startTime(CommonUtils.current())
        .value(0)
        .register();
  }

  /**
   * calculate the size of kafka record. NOTED: this method cares for only key and value in record
   *
   * @param record kafka record
   * @return size of record
   */
  static long sizeOf(ConnectRecord<?> record) {
    return sizeOf(record.key()) + sizeOf(record.value());
  }

  /**
   * estimate the size of object. Apart from row and byte array, other types have zero size.
   *
   * @param obj object
   * @return size of object
   */
  @VisibleForTesting
  static long sizeOf(Object obj) {
    if (obj instanceof byte[]) return ((byte[]) obj).length;
    else if (obj instanceof Row) return Serializer.ROW.to((Row) obj).length;
    else if (obj instanceof Boolean) return ByteUtils.SIZE_OF_BOOLEAN;
    else if (obj instanceof Short) return ByteUtils.SIZE_OF_SHORT;
    else if (obj instanceof Integer) return ByteUtils.SIZE_OF_INT;
    else if (obj instanceof Long) return ByteUtils.SIZE_OF_LONG;
    else if (obj instanceof Float) return ByteUtils.SIZE_OF_FLOAT;
    else if (obj instanceof Double) return ByteUtils.SIZE_OF_DOUBLE;
    else if (obj instanceof String) return ((String) obj).getBytes().length;
    else return 0;
  }

  private ConnectorUtils() {}
}
