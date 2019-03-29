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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.island.ohara.common.annotations.Nullable;
import com.island.ohara.common.annotations.Optional;
import com.island.ohara.common.util.CommonUtils;
import java.util.*;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.runtime.rest.entities.ConfigKeyInfo;

/**
 * This class is used to define the configuration of ohara connector. this class is related to
 * org.apache.kafka.connect.runtime.rest.entities.ConfigKeyInfo
 */
public class SettingDefinition implements JsonObject {
  // -------------------------------[groups]-------------------------------//
  public static final String CORE_GROUP = "core";
  private static final String COMMON_GROUP = "common";
  static final String ORDER_KEY = "order";
  static final String COLUMN_NAME_KEY = "name";
  static final String COLUMN_NEW_NAME_KEY = "newName";
  static final String COLUMN_DATA_TYPE_KEY = "dataType";
  // -------------------------------[default setting]-------------------------------//
  public static final SettingDefinition CONNECTOR_CLASS_DEFINITION =
      SettingDefinition.builder()
          .displayName("Connector class")
          .key("connector.class")
          .valueType(Type.CLASS)
          .documentation("the class name of connector")
          .group(CORE_GROUP)
          .orderInGroup(0)
          .build();
  public static final SettingDefinition TOPIC_NAMES_DEFINITION =
      SettingDefinition.builder()
          .displayName("Topics")
          .key("topics")
          .valueType(Type.LIST)
          .documentation("the topics used by connector")
          .reference(Reference.TOPIC)
          .group(CORE_GROUP)
          .orderInGroup(2)
          .build();
  public static final SettingDefinition NUMBER_OF_TASKS_DEFINITION =
      SettingDefinition.builder()
          .displayName("Number of tasks")
          .key("tasks.max")
          .valueType(Type.INT)
          .documentation("the number of tasks invoked by connector")
          .group(CORE_GROUP)
          .orderInGroup(3)
          .build();
  public static final SettingDefinition COLUMNS_DEFINITION =
      SettingDefinition.builder()
          .displayName("Schema")
          .key("columns")
          .valueType(Type.TABLE)
          .documentation("output schema")
          .optional()
          .group(CORE_GROUP)
          .orderInGroup(6)
          .tableKeys(
              Arrays.asList(ORDER_KEY, COLUMN_DATA_TYPE_KEY, COLUMN_NAME_KEY, COLUMN_NEW_NAME_KEY))
          .build();

  public static final SettingDefinition WORKER_CLUSTER_NAME_DEFINITION =
      SettingDefinition.builder()
          .displayName("worker cluster")
          .key("workerClusterName")
          .valueType(Type.STRING)
          .documentation(
              "the cluster name of running this connector."
                  + "If there is only one worker cluster, you can skip this setting since configurator will pick up a worker cluster for you")
          .reference(Reference.WORKER_CLUSTER)
          .group(CORE_GROUP)
          .optional()
          .orderInGroup(7)
          .build();
  public static final SettingDefinition KEY_CONVERTER_DEFINITION =
      SettingDefinition.builder()
          .displayName("key converter")
          .key("key.converter")
          .valueType(Type.CLASS)
          .documentation("key converter")
          .group(CORE_GROUP)
          .optional(ConverterType.NONE.className())
          .orderInGroup(4)
          .internal()
          .build();

  public static final SettingDefinition VALUE_CONVERTER_DEFINITION =
      SettingDefinition.builder()
          .displayName("value converter")
          .key("value.converter")
          .valueType(Type.STRING)
          .documentation("value converter")
          .group(CORE_GROUP)
          .optional(ConverterType.NONE.className())
          .orderInGroup(5)
          .internal()
          .build();

  public static final SettingDefinition VERSION_DEFINITION =
      SettingDefinition.builder()
          .displayName("version")
          .key("version")
          .valueType(Type.STRING)
          .documentation("version of connector")
          .group(CORE_GROUP)
          .optional("unknown")
          .orderInGroup(8)
          .readonly()
          .build();

  public static final SettingDefinition REVISION_DEFINITION =
      SettingDefinition.builder()
          .displayName("revision")
          .key("revision")
          .valueType(Type.STRING)
          .documentation("revision of connector")
          .group(CORE_GROUP)
          .optional("unknown")
          .orderInGroup(9)
          .readonly()
          .build();

  public static final SettingDefinition AUTHOR_DEFINITION =
      SettingDefinition.builder()
          .displayName("author")
          .key("author")
          .valueType(Type.STRING)
          .documentation("author of connector")
          .group(CORE_GROUP)
          .optional("unknown")
          .orderInGroup(10)
          .readonly()
          .build();

  /** this is the base of source/sink definition. */
  public static final SettingDefinition KIND_DEFINITION =
      SettingDefinition.builder()
          .displayName("kind")
          .key("kind")
          .valueType(Type.STRING)
          .documentation("kind of connector")
          .group(CORE_GROUP)
          .optional("connector")
          .orderInGroup(11)
          .readonly()
          .build();

  public static final SettingDefinition SOURCE_KIND_DEFINITION =
      SettingDefinition.builder(KIND_DEFINITION).optional("source").build();

  public static final SettingDefinition SINK_KIND_DEFINITION =
      SettingDefinition.builder(KIND_DEFINITION).optional("sink").build();

  // -------------------------------[reference]-------------------------------//
  enum Reference {
    NONE,
    TOPIC,
    WORKER_CLUSTER
  }
  // -------------------------------[type]-------------------------------//
  public enum Type {
    BOOLEAN,
    STRING,
    SHORT,
    INT,
    LONG,
    DOUBLE,
    LIST,
    CLASS,
    PASSWORD,
    TABLE
  }
  // -------------------------------[key]-------------------------------//
  private static final String REFERENCE_KEY = "reference";
  private static final String GROUP_KEP = "group";
  private static final String ORDER_IN_GROUP_KEY = "orderInGroup";
  private static final String DISPLAY_NAME_KEY = "displayName";
  private static final String EDITABLE_KEY = "editable";
  private static final String KEY_KEY = "key";
  private static final String VALUE_TYPE_KEY = "valueType";
  private static final String REQUIRED_KEY = "required";
  private static final String DEFAULT_VALUE_KEY = "defaultValue";
  private static final String DOCUMENTATION_KEY = "documentation";
  private static final String INTERNAL_KEY = "internal";
  private static final String TABLE_KEYS_KEY = "tableKeys";

  public static SettingDefinition ofJson(String json) {
    return JsonUtils.toObject(json, new TypeReference<SettingDefinition>() {});
  }

  private static ConfigDef.Type toType(Type type) {
    switch (type) {
      case BOOLEAN:
        return ConfigDef.Type.BOOLEAN;
      case STRING:
      case TABLE:
        return ConfigDef.Type.STRING;
      case SHORT:
        return ConfigDef.Type.SHORT;
      case INT:
        return ConfigDef.Type.INT;
      case LONG:
        return ConfigDef.Type.LONG;
      case DOUBLE:
        return ConfigDef.Type.DOUBLE;
      case LIST:
        return ConfigDef.Type.LIST;
      case CLASS:
        return ConfigDef.Type.CLASS;
      case PASSWORD:
        return ConfigDef.Type.PASSWORD;
      default:
        throw new UnsupportedOperationException("what is " + type);
    }
  }

  public static SettingDefinition of(ConfigKeyInfo configKeyInfo) {
    return SettingDefinition.ofJson(configKeyInfo.displayName());
  }

  private final String displayName;
  private final String group;
  private final int orderInGroup;
  private final boolean editable;
  private final String key;
  private final Type valueType;
  @Nullable private final String defaultValue;
  private final boolean required;
  private final String documentation;
  private final Reference reference;
  private final boolean internal;
  private final List<String> tableKeys;

  @JsonCreator
  private SettingDefinition(
      @JsonProperty(DISPLAY_NAME_KEY) String displayName,
      @JsonProperty(GROUP_KEP) String group,
      @JsonProperty(ORDER_IN_GROUP_KEY) int orderInGroup,
      @JsonProperty(EDITABLE_KEY) boolean editable,
      @JsonProperty(KEY_KEY) String key,
      @JsonProperty(VALUE_TYPE_KEY) String valueType,
      @JsonProperty(REQUIRED_KEY) boolean required,
      @Nullable @JsonProperty(DEFAULT_VALUE_KEY) String defaultValue,
      @JsonProperty(DOCUMENTATION_KEY) String documentation,
      @Nullable @JsonProperty(REFERENCE_KEY) String reference,
      @JsonProperty(INTERNAL_KEY) boolean internal,
      @JsonProperty(TABLE_KEYS_KEY) List<String> tableKeys) {
    this.displayName = CommonUtils.requireNonEmpty(displayName);
    this.group = group;
    this.orderInGroup = orderInGroup;
    this.editable = editable;
    this.key = CommonUtils.requireNonEmpty(key);
    this.valueType = Type.valueOf(Objects.requireNonNull(valueType));
    this.required = required;
    this.defaultValue = defaultValue;
    this.documentation = CommonUtils.requireNonEmpty(documentation);
    this.reference = Reference.valueOf(CommonUtils.requireNonEmpty(reference));
    this.internal = internal;
    this.tableKeys = Objects.requireNonNull(tableKeys);
  }

  @JsonProperty(INTERNAL_KEY)
  public boolean internal() {
    return internal;
  }

  @JsonProperty(DISPLAY_NAME_KEY)
  public String displayName() {
    return displayName;
  }

  @Nullable
  @JsonProperty(GROUP_KEP)
  public String group() {
    return group;
  }

  @JsonProperty(ORDER_IN_GROUP_KEY)
  public int orderInGroup() {
    return orderInGroup;
  }

  @JsonProperty(EDITABLE_KEY)
  public boolean editable() {
    return editable;
  }

  @JsonProperty(KEY_KEY)
  public String key() {
    return key;
  }

  @JsonProperty(VALUE_TYPE_KEY)
  public String valueType() {
    return valueType.name();
  }

  @JsonProperty(REQUIRED_KEY)
  public boolean required() {
    return required;
  }

  @Nullable
  @JsonProperty(DEFAULT_VALUE_KEY)
  public String defaultValue() {
    return defaultValue;
  }

  @JsonProperty(DOCUMENTATION_KEY)
  public String documentation() {
    return documentation;
  }

  @Nullable
  @JsonProperty(REFERENCE_KEY)
  public String reference() {
    return reference.name();
  }

  @JsonProperty(TABLE_KEYS_KEY)
  public List<String> tableKeys() {
    return new ArrayList<>(tableKeys);
  }

  public ConfigDef.ConfigKey toConfigKey() {
    return new ConfigDef.ConfigKey(
        key,
        toType(valueType),
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
        required() ? ConfigDef.NO_DEFAULT_VALUE : defaultValue(),
        null,
        ConfigDef.Importance.MEDIUM,
        documentation,
        group,
        orderInGroup,
        ConfigDef.Width.NONE,
        // we format ohara's definition to json and then put it in display_name.
        // This is a workaround to store our setting in kafka...
        toJsonString(),
        Collections.emptyList(),
        null,
        false);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof SettingDefinition)
      return toJsonString().equals(((SettingDefinition) obj).toJsonString());
    return false;
  }

  @Override
  public int hashCode() {
    return toJsonString().hashCode();
  }

  @Override
  public String toString() {
    return toJsonString();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static Builder builder(SettingDefinition definition) {
    return new Builder(definition);
  }

  public static class Builder {
    private String displayName;
    private String group = COMMON_GROUP;
    private int orderInGroup = -1;
    private boolean editable = true;
    private String key;
    private Type valueType = Type.STRING;
    private boolean required = true;
    @Nullable private String defaultValue = null;
    private String documentation = "this is no documentation for this setting";
    private Reference reference = Reference.NONE;
    private boolean internal = false;
    private List<String> tableKeys = Collections.emptyList();

    private Builder() {}

    private Builder(SettingDefinition definition) {
      this.displayName = definition.displayName;
      this.group = definition.group;
      this.orderInGroup = definition.orderInGroup;
      this.editable = definition.editable;
      this.key = definition.key;
      this.valueType = definition.valueType;
      this.required = definition.required;
      this.defaultValue = definition.defaultValue;
      this.documentation = definition.documentation;
      this.reference = definition.reference;
      this.internal = definition.internal;
      this.tableKeys = definition.tableKeys;
    }

    Builder internal() {
      this.internal = true;
      return this;
    }

    /**
     * this is a specific field fot Type.TABLE. It defines the keys' name for table.
     *
     * @param tableKeys key name of table
     * @return this builder
     */
    Builder tableKeys(List<String> tableKeys) {
      this.tableKeys = new ArrayList<>(CommonUtils.requireNonEmpty(tableKeys));
      return this;
    }

    public Builder key(String key) {
      this.key = CommonUtils.requireNonEmpty(key);
      this.displayName = this.key;
      return this;
    }

    @Optional("default type is STRING")
    public Builder valueType(Type valueType) {
      this.valueType = Objects.requireNonNull(valueType);
      return this;
    }

    @Optional("default is \"required!\" value")
    public Builder optional(String defaultValue) {
      this.required = false;
      this.defaultValue = Objects.requireNonNull(defaultValue);
      return this;
    }

    @Optional("default is \"required!\" value")
    public Builder optional() {
      this.required = false;
      this.defaultValue = null;
      return this;
    }

    @Optional("this is no documentation for this setting by default")
    public Builder documentation(String documentation) {
      this.documentation = CommonUtils.requireNonEmpty(documentation);
      return this;
    }

    /**
     * This property is required by ohara manager. There are some official setting having particular
     * control on UI.
     */
    @Optional("default is no reference")
    Builder reference(Reference reference) {
      this.reference = Objects.requireNonNull(reference);
      return this;
    }

    @Optional("default is common")
    public Builder group(String group) {
      this.group = CommonUtils.requireNonEmpty(group);
      return this;
    }

    @Optional("default is -1")
    public Builder orderInGroup(int orderInGroup) {
      this.orderInGroup = orderInGroup;
      return this;
    }

    @Optional("default is true")
    public Builder readonly() {
      this.editable = false;
      return this;
    }

    @Optional("default is key")
    public Builder displayName(String displayName) {
      this.displayName = CommonUtils.requireNonEmpty(displayName);
      return this;
    }

    public SettingDefinition build() {
      return new SettingDefinition(
          displayName,
          group,
          orderInGroup,
          editable,
          key,
          valueType.name(),
          required,
          defaultValue,
          documentation,
          reference.name(),
          internal,
          tableKeys);
    }
  }
}
