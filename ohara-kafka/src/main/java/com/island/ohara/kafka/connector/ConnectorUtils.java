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

import com.island.ohara.common.data.Cell;
import com.island.ohara.common.data.Column;
import com.island.ohara.common.data.Row;
import com.island.ohara.common.setting.SettingDef;
import com.island.ohara.common.setting.WithDefinitions;
import com.island.ohara.common.util.CommonUtils;
import com.island.ohara.kafka.connector.json.ConnectorDefUtils;
import com.island.ohara.metrics.basic.Counter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.ConnectRecord;

final class ConnectorUtils {
  static List<SettingDef> toSettingDefinitions(
      List<SettingDef> settingDefinitions, WithDefinitions def, boolean needColumnDefinition) {
    return Stream.of(
            settingDefinitions,
            Arrays.asList(
                ConnectorDefUtils.createVersionDefinition(def.version()),
                ConnectorDefUtils.createRevisionDefinition(def.revision()),
                ConnectorDefUtils.createAuthorDefinition(def.author())))
        .flatMap(Collection::stream)
        .filter(
            definition ->
                needColumnDefinition || definition != ConnectorDefUtils.COLUMNS_DEFINITION)
        .collect(Collectors.toList());
  }

  static ConfigDef toConfigDef(List<SettingDef> settingDefinitions) {
    ConfigDef def = new ConfigDef();
    settingDefinitions.stream().map(ConnectorDefUtils::toConfigKey).forEach(def::define);
    return def;
  }

  /**
   * Create and register a row counter with specific group name.
   *
   * @param group group name. It is normally equal to connector name
   * @return row counter
   */
  static Counter messageNumberCounter(String group) {
    return Counter.builder()
        .group(group)
        .name("message.number")
        .unit("messages")
        .document("number of messages")
        .value(0)
        .register();
  }

  /**
   * Create and register a size counter with specific group name.
   *
   * @param group group name. It is normally equal to connector name
   * @return size counter
   */
  static Counter messageSizeCounter(String group) {
    return Counter.builder()
        .group(group)
        .name("message.size")
        .unit("bytes")
        .document("size (in bytes) of messages")
        .value(0)
        .register();
  }

  /**
   * Create and register a number counter for ignored messages
   *
   * @param group group name. It is normally equal to connector name
   * @return number counter
   */
  static Counter ignoredMessageNumberCounter(String group) {
    return Counter.builder()
        .group(group)
        .name("ignored.message.number")
        .unit("messages")
        .document("number of ignored messages")
        .value(0)
        .register();
  }

  /**
   * Create and register a size counter for ignored messages
   *
   * @param group group name. It is normally equal to connector name
   * @return size counter
   */
  static Counter ignoredMessageSizeCounter(String group) {
    return Counter.builder()
        .group(group)
        .name("ignored.message.size")
        .unit("bytes")
        .document("size of ignored messages")
        .value(0)
        .register();
  }

  /**
   * compare the schema with input/output data.
   *
   * @param row row
   * @param columns columns
   */
  static boolean match(
      SettingDef.CheckRule rule,
      Row row,
      long rowSize,
      List<Column> columns,
      boolean isSink,
      Counter ignoredMessageNumberCounter,
      Counter ignoredMessageSizeCounter) {
    switch (rule) {
      case PERMISSIVE:
      case ENFORCING:
        try {
          ConnectorUtils.match(row, columns, isSink);
          return true;
        } catch (Throwable e) {
          if (rule == SettingDef.CheckRule.PERMISSIVE) {
            if (ignoredMessageNumberCounter != null) ignoredMessageNumberCounter.incrementAndGet();
            if (ignoredMessageSizeCounter != null) ignoredMessageSizeCounter.addAndGet(rowSize);
            return false;
          } else throw e;
        }
      case NONE:
      default:
        return true;
    }
  }

  /**
   * compare the schema with input/output data. this is a strict check that all columns MUST exist
   * and the input/output data can't have "unknown" column
   *
   * @param row row
   * @param columns columns
   */
  static void match(Row row, List<Column> columns, boolean isSink) {
    List<String> requiredNames =
        columns.stream()
            .map(column -> isSink ? column.name() : column.newName())
            .collect(Collectors.toList());

    if (!CommonUtils.isEmpty(columns)) {
      if (row.size() != columns.size())
        throw new IllegalArgumentException(
            "expected size:" + columns.size() + ", actual:" + row.size());
      List<String> dataColumnNames = row.names();
      dataColumnNames.forEach(
          name -> {
            if (requiredNames.stream().noneMatch(requiredName -> requiredName.equals(name)))
              throw new IllegalArgumentException(
                  "column name:"
                      + name
                      + " is not matched by schema:"
                      + String.join(",", requiredNames));
          });
      requiredNames.forEach(
          requiredName -> {
            if (dataColumnNames.stream().noneMatch(name -> name.equals(requiredName)))
              throw new IllegalArgumentException("there is not data for column:" + requiredName);
          });
      columns.forEach(
          column -> {
            Cell<?> cell = row.cell(isSink ? column.name() : column.newName());
            boolean match = false;
            switch (column.dataType()) {
              case BYTES:
                if (cell.value() instanceof byte[]) match = true;
                break;
              case BOOLEAN:
                if (cell.value() instanceof Boolean) match = true;
                break;
              case BYTE:
                if (cell.value() instanceof Byte) match = true;
                break;
              case SHORT:
                if (cell.value() instanceof Short) match = true;
                break;
              case INT:
                if (cell.value() instanceof Integer) match = true;
                break;
              case LONG:
                if (cell.value() instanceof Long) match = true;
                break;
              case FLOAT:
                if (cell.value() instanceof Float) match = true;
                break;
              case DOUBLE:
                if (cell.value() instanceof Double) match = true;
                break;
              case STRING:
                if (cell.value() instanceof String) match = true;
                break;
              case ROW:
                if (cell.value() instanceof Row) match = true;
                break;
              case OBJECT:
              default:
                if (cell.value() != null) match = true;
                break;
            }
            if (!match)
              throw new IllegalArgumentException(
                  "expected type: "
                      + column.dataType()
                      + ", actual:"
                      + cell.value().getClass().getName());
          });
    }
  }

  static long sizeOf(ConnectRecord<?> record) {
    if (record.key() instanceof byte[]) return ((byte[]) record.key()).length;
    else return 0;
  }

  private ConnectorUtils() {}
}
