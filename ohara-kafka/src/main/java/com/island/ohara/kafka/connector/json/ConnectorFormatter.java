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

package com.island.ohara.kafka.connector.json;

import com.island.ohara.common.annotations.Optional;
import com.island.ohara.common.data.Column;
import com.island.ohara.common.util.CommonUtils;
import com.island.ohara.kafka.connector.TaskConfig;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.kafka.common.config.ConfigDef;

/**
 * Kafka worker accept json and then unmarshal it to Map[String, String]. In most cases we can't
 * just use string to store our configuration. Hence, we needs a unified way to serialize non-string
 * type to string value. For example, number, list, and table.
 *
 * <p>The output of this formatter includes 1) TaskConfig -- used by Connector and Task 2) Request
 * of creating connector 3) Request of validating connector
 */
public final class ConnectorFormatter {
  // --------------------[KAFKA KEYs]--------------------//
  public static final String NAME_KEY = "name";
  static final int NAME_KEY_ORDER = 0;
  public static final String CLASS_NAME_KEY = "connector.class";
  static final int CLASS_NAME_KEY_ORDER = 1;
  public static final String TOPIC_NAMES_KEY = "topics";
  static final int TOPIC_NAMES_KEY_ORDER = 2;
  public static final String NUMBER_OF_TASKS_KEY = "tasks.max";
  static final int NUMBER_OF_TASKS_KEY_ORDER = 3;
  static final String KEY_CONVERTER_KEY = "key.converter";
  static final int KEY_CONVERTER_KEY_ORDER = 4;
  static final String VALUE_CONVERTER_KEY = "value.converter";
  static final int VALUE_CONVERTER_KEY_ORDER = 5;
  // --------------------[OHARA KEYs]--------------------//
  public static final String COLUMNS_KEY = "columns";
  static final int COLUMNS_KEY_ORDER = 6;
  public static final String WORKER_CLUSTER_NAME_KEY = "workerClusterName";
  static final int WORKER_CLUSTER_NAME_KEY_ORDER = 7;
  static final String VERSION_KEY = "version";
  static final int VERSION_KEY_ORDER = 8;
  static final String REVISION_KEY = "revision";
  static final int REVISION_KEY_ORDER = 9;
  static final String AUTHOR_KEY = "author";
  static final int AUTHOR_KEY_ORDER = 10;
  static final String CONNECTOR_TYPE_KEY = "connectorType";
  static final int CONNECTOR_TYPE_KEY_ORDER = 11;

  private static ConfigDef.ConfigKey toConfigKey(String key, String value, int order) {
    return SettingDefinition.newBuilder()
        .key(key)
        .readonly()
        .optional(value)
        .group(SettingDefinition.CORE_GROUP)
        .orderInGroup(order)
        .valueType(SettingDefinition.Type.STRING)
        .build()
        .toConfigKey();
  }

  public static ConfigDef toConfigDef(
      List<SettingDefinition> settingDefinitions,
      String version,
      String revision,
      String author,
      String connectorType) {
    ConfigDef def = new ConfigDef();
    settingDefinitions.stream().map(SettingDefinition::toConfigKey).forEach(def::define);
    def.define(toConfigKey(VERSION_KEY, Objects.requireNonNull(version), VERSION_KEY_ORDER));
    def.define(toConfigKey(REVISION_KEY, Objects.requireNonNull(revision), REVISION_KEY_ORDER));
    def.define(toConfigKey(AUTHOR_KEY, Objects.requireNonNull(author), AUTHOR_KEY_ORDER));
    def.define(
        toConfigKey(
            CONNECTOR_TYPE_KEY, Objects.requireNonNull(connectorType), CONNECTOR_TYPE_KEY_ORDER));
    return def;
  }

  public static ConnectorFormatter of() {
    return new ConnectorFormatter();
  }

  private final Map<String, String> settings = new HashMap<>();

  private ConnectorFormatter() {
    // ohara has custom serializeration so the json converter is useless for ohara
    converterTypeOfKey(ConverterType.NONE);
    converterTypeOfValue(ConverterType.NONE);
  }

  public ConnectorFormatter name(String name) {
    return setting(NAME_KEY, name);
  }

  public ConnectorFormatter setting(String key, String value) {
    System.out.println("[CHIA] key:" + key + " value:" + value);
    CommonUtils.requireNonEmpty(key, () -> "key can't be either empty or null");
    CommonUtils.requireNonEmpty(
        value, () -> "it is illegal to assign empty/null value to key:" + key);
    // Kafka has specific list format so we need to convert the string list ...
    try {
      List<String> ss = StringList.ofJson(value);
      // yep, this value is in json array
      settings.put(key, StringList.toKafkaString(ss));
    } catch (IllegalArgumentException e) {
      settings.put(key, value);
    }
    return this;
  }

  public ConnectorFormatter settings(Map<String, String> settings) {
    settings.forEach(this::setting);
    return this;
  }

  public ConnectorFormatter className(String className) {
    return setting(CLASS_NAME_KEY, className);
  }

  public ConnectorFormatter topicName(String topicName) {
    return topicNames(Collections.singletonList(topicName));
  }

  public ConnectorFormatter topicNames(List<String> topicNames) {
    return setting(TOPIC_NAMES_KEY, StringList.toKafkaString(topicNames));
  }

  public ConnectorFormatter numberOfTasks(int numberOfTasks) {
    return setting(NUMBER_OF_TASKS_KEY, String.valueOf(numberOfTasks));
  }

  @Optional("default is ConverterType.NONE")
  public ConnectorFormatter converterTypeOfKey(ConverterType type) {
    return setting(KEY_CONVERTER_KEY, type.className());
  }

  @Optional("default is ConverterType.NONE")
  public ConnectorFormatter converterTypeOfValue(ConverterType type) {
    return setting(VALUE_CONVERTER_KEY, type.className());
  }

  public ConnectorFormatter propGroups(String key, List<PropGroup> propGroups) {
    return setting(key, PropGroups.toString(propGroups));
  }

  public ConnectorFormatter column(Column column) {
    return columns(Collections.singletonList(column));
  }

  public ConnectorFormatter columns(List<Column> columns) {
    return propGroups(COLUMNS_KEY, PropGroups.of(columns));
  }

  public Creation requestOfCreation() {
    return Creation.of(new HashMap<>(settings));
  }

  public Map<String, String> requestOfValidation() {
    return new HashMap<>(settings);
  }

  public TaskConfig taskConfig() {
    return TaskConfig.of(new HashMap<>(settings));
  }
}
