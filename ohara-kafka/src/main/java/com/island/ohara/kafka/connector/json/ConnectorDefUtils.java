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

import static com.island.ohara.common.setting.SettingDef.COLUMN_DATA_TYPE_KEY;
import static com.island.ohara.common.setting.SettingDef.COLUMN_NAME_KEY;
import static com.island.ohara.common.setting.SettingDef.COLUMN_NEW_NAME_KEY;
import static com.island.ohara.common.setting.SettingDef.ORDER_KEY;

import com.island.ohara.common.annotations.VisibleForTesting;
import com.island.ohara.common.exception.OharaConfigException;
import com.island.ohara.common.setting.SettingDef;
import com.island.ohara.common.setting.SettingDef.Reference;
import com.island.ohara.common.setting.SettingDef.Type;
import com.island.ohara.common.setting.TableColumn;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.runtime.rest.entities.ConfigKeyInfo;

/**
 * This class is used to define the configuration of ohara connector. this class is related to
 * org.apache.kafka.connect.runtime.rest.entities.ConfigKeyInfo
 */
public final class ConnectorDefUtils {
  // -------------------------------[groups]-------------------------------//
  public static final String CORE_GROUP = "core";
  // -------------------------------[default setting]-------------------------------//
  private static final Map<String, SettingDef> _DEFINITIONS = new HashMap<>();

  private static SettingDef createDef(Function<SettingDef.Builder, SettingDef> f) {
    SettingDef settingDef =
        f.apply(SettingDef.builder().orderInGroup(_DEFINITIONS.size()).group(CORE_GROUP));
    // source kind and sink kind have identical key :)
    assert !(_DEFINITIONS.containsKey(settingDef.key()) && !settingDef.key().equals(KIND_KEY))
        : "duplicate key:" + settingDef.key() + " is illegal";
    _DEFINITIONS.put(settingDef.key(), settingDef);
    return settingDef;
  }

  /**
   * A internal field used to indicate the real group/name to connector. the name exists in both
   * Ohara and Kafka but it has different value to both as well...
   */
  public static final SettingDef CONNECTOR_KEY_DEFINITION =
      createDef(
          builder ->
              builder
                  .displayName("Connector key")
                  .key("connectorKey")
                  .required(Type.OBJECT_KEY)
                  .documentation("the key of this connector")
                  .internal()
                  .build());

  public static final SettingDef CONNECTOR_GROUP_DEFINITION =
      createDef(
          builder ->
              builder
                  .displayName("Connector group")
                  .key("group")
                  .optional("default")
                  .documentation("the group of this connector")
                  .build());

  /**
   * this is a embarrassed field to Ohara since we also have a filed called name for all objects.
   * our solution is to expose this field via definition but we always replace the value when
   * creating connector.
   */
  public static final SettingDef CONNECTOR_NAME_DEFINITION =
      createDef(
          builder ->
              builder
                  .displayName("Connector name")
                  .key("name")
                  .stringWithRandomDefault()
                  .documentation("the name of this connector")
                  .build());

  public static final SettingDef CONNECTOR_CLASS_DEFINITION =
      createDef(
          builder ->
              builder
                  .displayName("Connector class")
                  .key("connector.class")
                  .required(Type.CLASS)
                  .documentation("the class name of connector")
                  .build());

  public static final SettingDef TOPIC_KEYS_DEFINITION =
      createDef(
          builder ->
              builder
                  .displayName("Topics")
                  .key("topicKeys")
                  // we have to make this field optional since our UI needs to create connector
                  // without topics...
                  .optional(Type.OBJECT_KEYS)
                  .documentation("the topics used by connector")
                  .reference(Reference.TOPIC)
                  .build());

  public static final SettingDef TOPIC_NAMES_DEFINITION =
      createDef(
          builder ->
              builder
                  .displayName("Topics")
                  .key("topics")
                  .required(Type.ARRAY)
                  .documentation(
                      "the topic names in kafka form used by connector."
                          + "This field is internal and is generated from topicKeys. Normally, it is composed by group and name")
                  .reference(Reference.TOPIC)
                  .internal()
                  .build());
  public static final SettingDef NUMBER_OF_TASKS_DEFINITION =
      createDef(
          builder ->
              builder
                  .displayName("Number of tasks")
                  .key("tasks.max")
                  // TODO: use positive number instead (see
                  // https://github.com/oharastream/ohara/issues/3168)
                  .optional(1)
                  .documentation("the number of tasks invoked by connector")
                  .build());
  public static final SettingDef COLUMNS_DEFINITION =
      createDef(
          builder ->
              builder
                  .displayName("Schema")
                  .key("columns")
                  .documentation("the rules to connector in/out data")
                  .optional(
                      Arrays.asList(
                          TableColumn.builder()
                              .name(ORDER_KEY)
                              .type(TableColumn.Type.NUMBER)
                              .build(),
                          TableColumn.builder()
                              .name(COLUMN_DATA_TYPE_KEY)
                              .type(TableColumn.Type.STRING)
                              .build(),
                          TableColumn.builder()
                              .name(COLUMN_NAME_KEY)
                              .type(TableColumn.Type.STRING)
                              .build(),
                          TableColumn.builder()
                              .name(COLUMN_NEW_NAME_KEY)
                              .type(TableColumn.Type.STRING)
                              .build()))
                  .build());

  public static final SettingDef CHECK_RULE_DEFINITION =
      createDef(
          builder ->
              builder
                  .displayName("check rule")
                  .key("check.rule")
                  .documentation(
                      "the strategy for unmatched data. It includes enforcing, permissive and none")
                  .optional(
                      SettingDef.CheckRule.NONE.name(),
                      Stream.of(SettingDef.CheckRule.values())
                          .map(SettingDef.CheckRule::name)
                          .collect(Collectors.toSet()))
                  .build());

  public static final SettingDef WORKER_CLUSTER_KEY_DEFINITION =
      createDef(
          builder ->
              builder
                  .displayName("worker cluster")
                  .key("workerClusterKey")
                  .required(Type.OBJECT_KEY)
                  .documentation("the cluster name of running this connector.")
                  .reference(Reference.WORKER_CLUSTER)
                  .build());

  public static final SettingDef KEY_CONVERTER_DEFINITION =
      createDef(
          builder ->
              builder
                  .displayName("key converter")
                  .key("key.converter")
                  .documentation("key converter")
                  .optionalClassValue(ConverterType.NONE.className())
                  .internal()
                  .build());

  public static final SettingDef VALUE_CONVERTER_DEFINITION =
      createDef(
          builder ->
              builder
                  .displayName("value converter")
                  .key("value.converter")
                  .documentation("value converter")
                  .optionalClassValue(ConverterType.NONE.className())
                  .internal()
                  .build());

  /** this is the base of source/sink definition. */
  public static final String KIND_KEY = "kind";

  public static final String SOURCE_CONNECTOR = "source";
  public static final String SINK_CONNECTOR = "sink";

  public static final SettingDef SOURCE_KIND_DEFINITION =
      createDef(
          builder ->
              builder
                  .displayName(KIND_KEY)
                  .key(KIND_KEY)
                  .documentation("kind of connector")
                  .optional(SOURCE_CONNECTOR)
                  .readonly()
                  .build());

  public static final SettingDef SINK_KIND_DEFINITION =
      createDef(
          builder ->
              builder
                  .displayName(SOURCE_KIND_DEFINITION.displayName())
                  .key(SOURCE_KIND_DEFINITION.key())
                  .documentation(SOURCE_KIND_DEFINITION.documentation())
                  .group(SOURCE_KIND_DEFINITION.group())
                  .optional(SINK_CONNECTOR)
                  .orderInGroup(SOURCE_KIND_DEFINITION.orderInGroup())
                  .readonly()
                  .build());

  public static final SettingDef TAGS_DEFINITION =
      createDef(
          builder ->
              builder
                  .displayName("tags")
                  .key("tags")
                  .optional(Type.TAGS)
                  .documentation("tags to this connector")
                  .build());

  static final int VERSION_ORDER = _DEFINITIONS.size();
  public static final String VERSION_KEY = "version";

  public static SettingDef createVersionDefinition(String version) {
    return SettingDef.builder()
        .displayName(VERSION_KEY)
        .key(VERSION_KEY)
        .documentation("version of connector")
        .group(CORE_GROUP)
        .optional(version)
        .orderInGroup(VERSION_ORDER)
        .readonly()
        .build();
  }

  static final int REVISION_ORDER = VERSION_ORDER + 1;
  public static final String REVISION_KEY = "revision";

  public static SettingDef createRevisionDefinition(String revision) {
    return SettingDef.builder()
        .displayName(REVISION_KEY)
        .key(REVISION_KEY)
        .documentation("revision of connector")
        .group(CORE_GROUP)
        .optional(revision)
        .orderInGroup(REVISION_ORDER)
        .readonly()
        .build();
  }

  static final int AUTHOR_ORDER = REVISION_ORDER + 1;
  public static final String AUTHOR_KEY = "author";

  public static SettingDef createAuthorDefinition(String author) {
    return SettingDef.builder()
        .displayName(AUTHOR_KEY)
        .key(AUTHOR_KEY)
        .documentation("author of connector")
        .group(CORE_GROUP)
        .optional(author)
        .orderInGroup(AUTHOR_ORDER)
        .readonly()
        .build();
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
      case OBJECT_KEYS:
      case OBJECT_KEY:
      case TAGS:
        return ConfigDef.Type.STRING;
      case POSITIVE_SHORT:
      case SHORT:
        return ConfigDef.Type.SHORT;
      case PORT:
      case BINDING_PORT:
      case POSITIVE_INT:
      case INT:
        return ConfigDef.Type.INT;
      case POSITIVE_LONG:
      case LONG:
        return ConfigDef.Type.LONG;
      case POSITIVE_DOUBLE:
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
        def.necessary() == SettingDef.Necessary.REQUIRED
            ? ConfigDef.NO_DEFAULT_VALUE
            : def.defaultValue(),
        (String key, Object value) -> {
          // TODO move this to RouteUtils in #2191
          try {
            def.checker().accept(value);
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
      _DEFINITIONS.values().stream()
          // the kind is different between source and sink so we include it from default definitions
          .filter(d -> !d.key().equals(KIND_KEY))
          .collect(Collectors.toList());

  // disable constructor
  private ConnectorDefUtils() {}
}
