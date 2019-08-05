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

import static com.island.ohara.common.setting.SettingDef.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.island.ohara.common.annotations.VisibleForTesting;
import com.island.ohara.common.exception.OharaConfigException;
import com.island.ohara.common.json.JsonUtils;
import com.island.ohara.common.setting.SettingDef;
import com.island.ohara.common.setting.SettingDef.Reference;
import com.island.ohara.common.setting.SettingDef.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.runtime.rest.entities.ConfigKeyInfo;

/**
 * This class is used to define the configuration of ohara connector. this class is related to
 * org.apache.kafka.connect.runtime.rest.entities.ConfigKeyInfo
 */
public abstract class ConnectorDefinitions {
  // -------------------------------[groups]-------------------------------//
  public static final String CORE_GROUP = "core";
  // -------------------------------[default setting]-------------------------------//
  private static final AtomicInteger ORDER_COUNTER = new AtomicInteger(0);

  public static final SettingDef CONNECTOR_KEY_DEFINITION =
      SettingDef.builder()
          .displayName("Connector key")
          .key("connectorKey")
          .valueType(Type.CONNECTOR_KEY)
          .documentation("the key of this connector")
          .group(CORE_GROUP)
          .orderInGroup(ORDER_COUNTER.getAndIncrement())
          .build();

  // TODO this is a workaround way to enable custom checker
  // Please refactor this in https://github.com/oharastream/ohara/issues/2124...by Sam
  static final Consumer<Object> chekerOfConnectorKey =
      (Object value) -> {
        if (value instanceof String) {
          try {
            // try parse the json string to Connector Key
            ConnectorKey.ofJsonString((String) value);
            // pass
          } catch (Exception e) {
            throw new OharaConfigException(
                "can't be converted to CONNECTOR_KEY type. since:" + e.getMessage());
          }
        } else throw new OharaConfigException("the configured value must be String type");
      };
  /** this setting is mapped to kafka's name. */
  public static final SettingDef CONNECTOR_NAME_DEFINITION =
      SettingDef.builder()
          .displayName("Connector name")
          .key("name")
          .valueType(Type.STRING)
          .documentation("the name of this connector")
          .group(CORE_GROUP)
          .orderInGroup(ORDER_COUNTER.getAndIncrement())
          .internal()
          .build();

  public static final SettingDef CONNECTOR_CLASS_DEFINITION =
      SettingDef.builder()
          .displayName("Connector class")
          .key("connector.class")
          .valueType(Type.CLASS)
          .documentation("the class name of connector")
          .group(CORE_GROUP)
          .orderInGroup(ORDER_COUNTER.getAndIncrement())
          .build();

  public static final SettingDef TOPIC_KEYS_DEFINITION =
      SettingDef.builder()
          .displayName("Topics")
          .key("topicKeys")
          .valueType(Type.TOPIC_KEYS)
          .documentation("the topics used by connector")
          .reference(Reference.TOPIC)
          .group(CORE_GROUP)
          .orderInGroup(ORDER_COUNTER.getAndIncrement())
          .build();

  // TODO this is a workaround way to enable custom checker
  // Please refactor this in https://github.com/oharastream/ohara/issues/2124...by Sam
  static final Consumer<Object> chekerOfTopicKeys =
      (Object value) -> {
        if (value instanceof String) {
          try {
            if (JsonUtils.toObject((String) value, new TypeReference<List<KeyImpl>>() {}).isEmpty())
              throw new OharaConfigException("TOPIC_KEYS can't be empty!!!");
          } catch (Exception e) {
            throw new OharaConfigException(
                "can't be converted to TOPIC_KEYS type. since:" + e.getMessage());
          }
        } else throw new OharaConfigException("the configured value must be String type");
      };

  public static final SettingDef TOPIC_NAMES_DEFINITION =
      SettingDef.builder()
          .displayName("Topics")
          .key("topics")
          .valueType(Type.ARRAY)
          .documentation(
              "the topic names in kafka form used by connector."
                  + "This field is internal and is generated from topicKeys. Normally, it is composed by group and name")
          .reference(Reference.TOPIC)
          .group(CORE_GROUP)
          .orderInGroup(ORDER_COUNTER.getAndIncrement())
          .internal()
          .build();
  public static final SettingDef NUMBER_OF_TASKS_DEFINITION =
      SettingDef.builder()
          .displayName("Number of tasks")
          .key("tasks.max")
          .valueType(Type.INT)
          .documentation("the number of tasks invoked by connector")
          .group(CORE_GROUP)
          .orderInGroup(ORDER_COUNTER.getAndIncrement())
          .build();
  public static final SettingDef COLUMNS_DEFINITION =
      SettingDef.builder()
          .displayName("Schema")
          .key("columns")
          .valueType(Type.TABLE)
          .documentation("output schema")
          .optional()
          .group(CORE_GROUP)
          .orderInGroup(ORDER_COUNTER.getAndIncrement())
          .tableKeys(
              Arrays.asList(ORDER_KEY, COLUMN_DATA_TYPE_KEY, COLUMN_NAME_KEY, COLUMN_NEW_NAME_KEY))
          .build();

  public static final SettingDef WORKER_CLUSTER_NAME_DEFINITION =
      SettingDef.builder()
          .displayName("worker cluster")
          .key("workerClusterName")
          .valueType(Type.STRING)
          .documentation(
              "the cluster name of running this connector."
                  + "If there is only one worker cluster, you can skip this setting since configurator will pick up a worker cluster for you")
          .reference(Reference.WORKER_CLUSTER)
          .group(CORE_GROUP)
          .optional()
          .orderInGroup(ORDER_COUNTER.getAndIncrement())
          .build();
  public static final SettingDef KEY_CONVERTER_DEFINITION =
      SettingDef.builder()
          .displayName("key converter")
          .key("key.converter")
          .valueType(Type.CLASS)
          .documentation("key converter")
          .group(CORE_GROUP)
          .optional(ConverterType.NONE.className())
          .orderInGroup(ORDER_COUNTER.getAndIncrement())
          .internal()
          .build();

  public static final SettingDef VALUE_CONVERTER_DEFINITION =
      SettingDef.builder()
          .displayName("value converter")
          .key("value.converter")
          .valueType(Type.STRING)
          .documentation("value converter")
          .group(CORE_GROUP)
          .optional(ConverterType.NONE.className())
          .orderInGroup(ORDER_COUNTER.getAndIncrement())
          .internal()
          .build();

  public static final SettingDef VERSION_DEFINITION =
      SettingDef.builder()
          .displayName("version")
          .key("version")
          .valueType(Type.STRING)
          .documentation("version of connector")
          .group(CORE_GROUP)
          .optional("unknown")
          .orderInGroup(ORDER_COUNTER.getAndIncrement())
          .readonly()
          .build();

  public static final SettingDef REVISION_DEFINITION =
      SettingDef.builder()
          .displayName("revision")
          .key("revision")
          .valueType(Type.STRING)
          .documentation("revision of connector")
          .group(CORE_GROUP)
          .optional("unknown")
          .orderInGroup(ORDER_COUNTER.getAndIncrement())
          .readonly()
          .build();

  public static final SettingDef AUTHOR_DEFINITION =
      SettingDef.builder()
          .displayName("author")
          .key("author")
          .valueType(Type.STRING)
          .documentation("author of connector")
          .group(CORE_GROUP)
          .optional("unknown")
          .orderInGroup(ORDER_COUNTER.getAndIncrement())
          .readonly()
          .build();

  /** this is the base of source/sink definition. */
  public static final SettingDef KIND_DEFINITION =
      SettingDef.builder()
          .displayName("kind")
          .key("kind")
          .valueType(Type.STRING)
          .documentation("kind of connector")
          .group(CORE_GROUP)
          .optional("connector")
          .orderInGroup(ORDER_COUNTER.getAndIncrement())
          .readonly()
          .build();

  public static final SettingDef SOURCE_KIND_DEFINITION =
      SettingDef.builder(KIND_DEFINITION).optional("source").build();

  public static final SettingDef SINK_KIND_DEFINITION =
      SettingDef.builder(KIND_DEFINITION).optional("sink").build();

  public static final SettingDef TAGS_DEFINITION =
      SettingDef.builder()
          .displayName("tags")
          .key("tags")
          .valueType(Type.TAGS)
          .documentation("tags to this connector")
          .group(CORE_GROUP)
          // the tags in connector
          .internal()
          .optional()
          .orderInGroup(ORDER_COUNTER.getAndIncrement())
          .build();

  // Use to check the custom checker for specific value
  @VisibleForTesting
  static void check(Consumer<Object> consumer, Object value) {
    consumer.accept(value);
  }

  @VisibleForTesting
  static ConfigDef.Type toType(Type type) {
    switch (type) {
      case BOOLEAN:
        return ConfigDef.Type.BOOLEAN;
      case JDBC_TABLE:
      case STRING:
      case DURATION:
      case TABLE:
      case TOPIC_KEYS:
      case CONNECTOR_KEY:
      case TAGS:
        return ConfigDef.Type.STRING;
      case SHORT:
        return ConfigDef.Type.SHORT;
      case PORT:
      case INT:
        return ConfigDef.Type.INT;
      case LONG:
        return ConfigDef.Type.LONG;
      case DOUBLE:
        return ConfigDef.Type.DOUBLE;
      case ARRAY:
        return ConfigDef.Type.LIST;
      case CLASS:
        return ConfigDef.Type.CLASS;
      case PASSWORD:
        return ConfigDef.Type.PASSWORD;
      default:
        throw new UnsupportedOperationException("what is " + type);
    }
  }

  public static SettingDef of(ConfigKeyInfo configKeyInfo) {
    return SettingDef.ofJson(configKeyInfo.displayName());
  }

  public static ConfigDef.ConfigKey toConfigKey(SettingDef def) {
    return new ConfigDef.ConfigKey(
        def.key(),
        toType(def.valueType()),
        // There are three kind of argumentDefinition.
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
        def.required() ? ConfigDef.NO_DEFAULT_VALUE : def.defaultValue(),
        (String key, Object value) -> {
          if (def.required() && value == null)
            // this is the kafka internal validate catch exception
            // do not change this exception class unless you know what you did
            throw new ConfigException(key + " is required!");
          if (value == null) return;
          try {
            if (def.valueType().equals(Type.CONNECTOR_KEY)) chekerOfConnectorKey.accept(value);
            else if (def.valueType().equals(Type.TOPIC_KEYS)) chekerOfTopicKeys.accept(value);
            else def.checker().accept(value);
          } catch (OharaConfigException e) {
            // wrap OharaConfigException to ConfigException in order to pass this checker to kafka
            throw new ConfigException(e.getMessage());
          } catch (Throwable e) {
            // Except for ConfigException, other exceptions are not allowed by kafka.
            throw new OharaConfigException(key, value, e.getMessage());
          }
        },
        ConfigDef.Importance.MEDIUM,
        def.documentation(),
        def.group(),
        def.orderInGroup(),
        ConfigDef.Width.NONE,
        // we format ohara's definition to json and then put it in display_name.
        // This is a workaround to store our setting in kafka...
        def.toString(),
        Collections.emptyList(),
        null,
        false);
  }

  // --------------------------[helper method]------------------------------//
  /** the default definitions for all ohara connector. */
  public static final List<SettingDef> DEFINITIONS_DEFAULT =
      Arrays.asList(
          ConnectorDefinitions.CONNECTOR_NAME_DEFINITION,
          ConnectorDefinitions.CONNECTOR_KEY_DEFINITION,
          ConnectorDefinitions.CONNECTOR_CLASS_DEFINITION,
          ConnectorDefinitions.COLUMNS_DEFINITION,
          ConnectorDefinitions.KEY_CONVERTER_DEFINITION,
          ConnectorDefinitions.VALUE_CONVERTER_DEFINITION,
          ConnectorDefinitions.WORKER_CLUSTER_NAME_DEFINITION,
          ConnectorDefinitions.NUMBER_OF_TASKS_DEFINITION,
          ConnectorDefinitions.TOPIC_KEYS_DEFINITION,
          ConnectorDefinitions.TOPIC_NAMES_DEFINITION,
          ConnectorDefinitions.TAGS_DEFINITION);

  /**
   * find the default value of version from settings
   *
   * @param settingDefinitions settings
   * @return default value of version. Otherwise, NoSuchElementException will be thrown
   */
  public static String version(List<SettingDef> settingDefinitions) {
    return defaultValue(settingDefinitions, ConnectorDefinitions.VERSION_DEFINITION.key());
  }

  /**
   * find the default value of revision from settings
   *
   * @param settingDefinitions settings
   * @return default value of revision. Otherwise, NoSuchElementException will be thrown
   */
  public static String revision(List<SettingDef> settingDefinitions) {
    return defaultValue(settingDefinitions, ConnectorDefinitions.REVISION_DEFINITION.key());
  }

  /**
   * find the default value of author from settings
   *
   * @param settingDefinitions settings
   * @return default value of author. Otherwise, NoSuchElementException will be thrown
   */
  public static String author(List<SettingDef> settingDefinitions) {
    return defaultValue(settingDefinitions, ConnectorDefinitions.AUTHOR_DEFINITION.key());
  }

  /**
   * find the default value of type name from settings
   *
   * @param settingDefinitions settings
   * @return default value of type name. Otherwise, NoSuchElementException will be thrown
   */
  public static String kind(List<SettingDef> settingDefinitions) {
    return defaultValue(settingDefinitions, ConnectorDefinitions.KIND_DEFINITION.key());
  }

  private static String defaultValue(List<SettingDef> settingDefinitions, String key) {
    return Optional.ofNullable(
            settingDefinitions.stream()
                .filter(s -> s.key().equals(key))
                .findAny()
                .orElseGet(
                    () -> {
                      throw new NoSuchElementException(
                          key + " doesn't exist! Are you using a stale worker image?");
                    })
                .defaultValue())
        .orElseGet(
            () -> {
              throw new NoSuchElementException(
                  "there is no value matched to " + key + ". Are you using a stale worker image?");
            });
  }

  // disable constructor
  private ConnectorDefinitions() {}
}
