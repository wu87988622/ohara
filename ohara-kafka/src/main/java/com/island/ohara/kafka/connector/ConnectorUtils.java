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
import com.island.ohara.common.data.Column;
import com.island.ohara.common.data.DataType;
import com.island.ohara.common.util.CommonUtil;
import com.island.ohara.common.util.VersionUtil;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.kafka.common.config.ConfigDef;

public class ConnectorUtils {
  public static final String NAME_KEY = "name";
  public static final String CONNECTOR_CLASS_KEY = "connector.class";
  public static final String TOPIC_NAMES_KEY = "topics";
  public static final String NUMBER_OF_TASKS_KEY = "tasks.max";
  // kafka connector accept only Map[String, String] as input arguments so we have to serialize the
  // column to a string
  // TODO: Personally, I hate this ugly workaround...by chia
  public static final String COLUMNS_KEY = "schema";

  /** the default definitions for all ohara connector. */
  @VisibleForTesting
  static final List<Definition> DEFINITIONS_DEFAULT =
      Arrays.asList(
          Definition.newBuilder()
              .name(NAME_KEY)
              .valueType(Definition.Type.STRING)
              .documentation("the name of connector")
              .required()
              .build(),
          Definition.newBuilder()
              .name(CONNECTOR_CLASS_KEY)
              .valueType(Definition.Type.CLASS)
              .documentation("the full name of connector")
              .required()
              .build(),
          Definition.newBuilder()
              .name(TOPIC_NAMES_KEY)
              .valueType(Definition.Type.LIST)
              .documentation("the topics used by connector")
              .required()
              .build(),
          Definition.newBuilder()
              .name(NUMBER_OF_TASKS_KEY)
              .valueType(Definition.Type.INT)
              .documentation("the number of tasks invoked by connector")
              .optional(1)
              .build(),
          Definition.newBuilder()
              .name(COLUMNS_KEY)
              .valueType(Definition.Type.LIST)
              .documentation("the number of tasks invoked by connector")
              .optional()
              .build());

  @VisibleForTesting
  static TaskConfig toTaskConfig(Map<String, String> props) {
    return TaskConfig.builder()
        .name(
            Optional.ofNullable(props.get(NAME_KEY))
                .orElseGet(
                    () -> {
                      throw new IllegalArgumentException(NAME_KEY + " doesn't exist!!!");
                    }))
        .topics(
            Optional.ofNullable(props.get(TOPIC_NAMES_KEY))
                .map(s -> Arrays.asList(s.split(",")))
                .orElseGet(
                    () -> {
                      throw new IllegalArgumentException(TOPIC_NAMES_KEY + " doesn't exist!!!");
                    }))
        .columns(ConnectorUtils.toColumns(props.get(ConnectorUtils.COLUMNS_KEY)))
        .options(props)
        .build();
  }

  @VisibleForTesting
  static Map<String, String> toMap(TaskConfig taskConfig) {
    // NOTED: the passed props is not a "copy" so any changes to props will impact props itself.
    CommonUtil.requireNonEmpty(taskConfig.topics());
    Map<String, String> map = new HashMap<>(taskConfig.options());
    map.put(COLUMNS_KEY, fromColumns(taskConfig.columns()));
    map.put(TOPIC_NAMES_KEY, String.join(",", taskConfig.topics()));
    map.put(NAME_KEY, taskConfig.name());
    return map;
  }
  /**
   * this version is exposed to kafka connector. Kafka connector's version mechanism carry a string
   * used to represent the "version" only. It is a such weak function which can't carry other
   * information - ex. revision. Hence, we do a magic way to combine the revision with version and
   * then parse it manually in order to provide more powerful CLUSTER APIs (see ClusterRoute)
   */
  public static final String VERSION = VersionUtil.VERSION + "_" + VersionUtil.REVISION;

  private static ConfigDef.Type toType(Definition.Type type) {
    switch (type) {
      case BOOLEAN:
        return ConfigDef.Type.BOOLEAN;
      case STRING:
        return ConfigDef.Type.STRING;
      case SHORT:
        return ConfigDef.Type.SHORT;
      case INT:
        return ConfigDef.Type.INT;
      case LONG:
        return ConfigDef.Type.LONG;
      case DOUBLE:
        return ConfigDef.Type.STRING;
      case CLASS:
        return ConfigDef.Type.CLASS;
      case LIST:
        return ConfigDef.Type.LIST;
      case PASSWORD:
        return ConfigDef.Type.PASSWORD;
      default:
        throw new UnsupportedOperationException("what is " + type);
    }
  }

  private static void update(ConfigDef def, Definition definition) {
    def.define(
        definition.name(),
        ConnectorUtils.toType(definition.valueType()),
        // There are three kind of definition.
        // 1) required -- this case MUST has no default value
        // 2) optional with default value -- user doesn't need to define value since there is
        // already one.
        //    for example, the default of tasks.max is 1.
        // 3) optional without default value -- user doesn't need to define value even though there
        // is no default
        //    for example, the columns have no default value but you can still skip the assignment
        // since the connector
        //    should skip the column process if no specific columns exist.
        // Kafka doesn't provide a flag to represent the "required" or "optional". By contrast, it
        // provides a specific
        // object to help developer to say "I have no default value...."
        // for case 1) -- we have to assign ConfigDef.NO_DEFAULT_VALUE
        // for case 2) -- we have to assign the default value
        // for case 3) -- we have to assign null
        // Above rules are important to us since we depends on the validation from kafka. We will
        // retrieve a wrong
        // report from kafka if we don't follow the rule.
        definition.required() ? ConfigDef.NO_DEFAULT_VALUE : definition.valueDefault().orElse(null),
        ConfigDef.Importance.HIGH,
        definition.documentation());
  }

  /**
   * @param definitions ohara's definitions
   * @return ConfigDef
   */
  @VisibleForTesting
  static ConfigDef toConfigDefWithDefault(List<Definition> definitions) {
    return toConfigDef(definitions, true);
  }

  /**
   * Convert our definitions to kafka's ConfigDef
   *
   * @param definitions ohara's definitions
   * @param withDefault true if you want to integrate default definitions to ConfigDef
   * @return ConfigDef
   */
  @VisibleForTesting
  static ConfigDef toConfigDef(List<Definition> definitions, boolean withDefault) {
    ConfigDef def = new ConfigDef();
    definitions.forEach(definition -> update(def, definition));
    if (withDefault) DEFINITIONS_DEFAULT.forEach(definition -> update(def, definition));
    return def;
  }

  /**
   * Column object serializes to String It uses "," to join all fields and concat Columns.
   *
   * @param columns Column list
   * @return a serialized string
   */
  public static String fromColumns(List<Column> columns) {
    columns.forEach(
        column -> {
          if (column.name().contains(","))
            throw new IllegalArgumentException("\",\" is not illegal");
          if (column.newName().contains(","))
            throw new IllegalArgumentException("\",\" is not illegal");
        });
    return columns
        .stream()
        .map(
            c -> Arrays.asList(c.name(), c.newName(), c.dataType().name, String.valueOf(c.order())))
        .map(list -> String.join(",", list))
        .collect(Collectors.joining(","));
  }

  /**
   * Deserialized frome String It is split by ","
   *
   * @param columnsString generated by fromColumns
   * @return Column list
   */
  @VisibleForTesting
  static List<Column> toColumns(String columnsString) {
    if (columnsString == null || columnsString.isEmpty()) return Collections.emptyList();
    else {
      int tupleLength = 4;
      String[] splits = columnsString.split(",");
      if (splits.length % tupleLength != 0)
        throw new IllegalArgumentException(
            String.format("invalid format from columns string:%s", columnsString));
      else {
        return Stream.iterate(0, i -> i + tupleLength)
            .limit(splits.length / tupleLength)
            .map(
                x ->
                    Column.newBuilder()
                        .name(splits[x])
                        .newName(splits[x + 1])
                        .dataType(DataType.of(splits[x + 2]))
                        .order(Integer.parseInt(splits[x + 3]))
                        .build())
            .collect(Collectors.toList());
      }
    }
  }
}
