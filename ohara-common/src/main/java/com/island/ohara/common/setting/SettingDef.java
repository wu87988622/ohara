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

package com.island.ohara.common.setting;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.island.ohara.common.annotations.Nullable;
import com.island.ohara.common.annotations.Optional;
import com.island.ohara.common.exception.OharaConfigException;
import com.island.ohara.common.json.JsonObject;
import com.island.ohara.common.json.JsonUtils;
import com.island.ohara.common.util.CommonUtils;
import java.io.Serializable;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * This class is the base class to define configuration for ohara object.
 *
 * <p>SettingDef is stored by Configurator Store now, and the serialization is based on java
 * serializable. Hence, we add this Serializable interface here.
 */
public class SettingDef implements JsonObject, Serializable {
  private static final long serialVersionUID = 1L;
  // -------------------------------[groups]-------------------------------//
  public static final String COMMON_GROUP = "common";
  public static final String ORDER_KEY = "order";
  public static final String COLUMN_NAME_KEY = "name";
  public static final String COLUMN_NEW_NAME_KEY = "newName";
  public static final String COLUMN_DATA_TYPE_KEY = "dataType";
  // -------------------------------[reference]-------------------------------//
  public enum Reference {
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
    /**
     * ARRAY is a better naming than LIST as LIST has another meaning to ohara manager.
     *
     * <p>[ "a1", "a2", "a3" ]
     */
    ARRAY,
    CLASS,
    PASSWORD,
    /**
     * JDBC_TABLE is a specific string type used to reminder Ohara Manager that this field requires
     * a **magic** button to show available tables of remote database via Query APIs. Except for the
     * **magic** in UI, there is no other stuff for this JDBC_TYPE since kafka can't verify the
     * input arguments according to other arguments. It means we can't connect to remote database to
     * check the existence of input table.
     */
    JDBC_TABLE,
    TABLE,
    /**
     * The formats accepted are based on the ISO-8601 duration format PnDTnHnMn.nS with days
     * considered to be exactly 24 hours. Please reference to
     * https://docs.oracle.com/javase/8/docs/api/java/time/Duration.html#parse-java.lang.CharSequence-
     */
    DURATION,
    /** The legal range for port is [1, 65535]. */
    PORT,
    /**
     * The legal range for port is [1, 65535]. The difference between PORT and BINDING_PORT is that
     * we will check the availability for the BINDING_PORT.
     */
    BINDING_PORT,
    /** { "group": "default", "name":" name.jar" } */
    JAR_KEY,
    /** [ { "group": "g", "name":" n" } ] */
    TOPIC_KEYS,
    /** { "group": "g0", "name": "n0" } */
    CONNECTOR_KEY,
    /**
     * TAGS is a flexible type accepting a json representation. For example:
     *
     * <p>{ "k0": "v0", "k1": "v1", "k2": ["a0", "b0" ] }
     */
    TAGS,
  }

  // -------------------------------[key]-------------------------------//
  private static final String REFERENCE_KEY = "reference";
  private static final String GROUP_KEY = "group";
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

  public static SettingDef ofJson(String json) {
    return JsonUtils.toObject(json, new TypeReference<SettingDef>() {});
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
  private SettingDef(
      @JsonProperty(DISPLAY_NAME_KEY) String displayName,
      @JsonProperty(GROUP_KEY) String group,
      @JsonProperty(ORDER_IN_GROUP_KEY) int orderInGroup,
      @JsonProperty(EDITABLE_KEY) boolean editable,
      @JsonProperty(KEY_KEY) String key,
      @JsonProperty(VALUE_TYPE_KEY) Type valueType,
      @JsonProperty(REQUIRED_KEY) boolean required,
      @Nullable @JsonProperty(DEFAULT_VALUE_KEY) String defaultValue,
      @JsonProperty(DOCUMENTATION_KEY) String documentation,
      @Nullable @JsonProperty(REFERENCE_KEY) Reference reference,
      @JsonProperty(INTERNAL_KEY) boolean internal,
      @JsonProperty(TABLE_KEYS_KEY) List<String> tableKeys) {
    this.group = CommonUtils.requireNonEmpty(group);
    this.orderInGroup = orderInGroup;
    this.editable = editable;
    this.key = CommonUtils.requireNonEmpty(key);
    this.valueType = Objects.requireNonNull(valueType);
    this.required = required;
    this.defaultValue = defaultValue;
    this.documentation = CommonUtils.requireNonEmpty(documentation);
    this.reference = Objects.requireNonNull(reference);
    this.internal = internal;
    this.tableKeys = Objects.requireNonNull(tableKeys);
    // It is legal to ignore the display name.
    // However, we all hate null so we set the default value equal to key.
    this.displayName = CommonUtils.isEmpty(displayName) ? this.key : displayName;
  }

  /**
   * Generate official checker according to input type.
   *
   * @return checker
   */
  public Consumer<Object> checker() {
    return (Object value) -> {
      // we don't check the optional key with null value
      if (!this.required && this.defaultValue == null && value == null) return;

      // besides the first rule, any other combination of settings should check the value is not
      // null (default or from api)
      final Object trueValue = value == null ? this.defaultValue : value;
      if (trueValue == null)
        throw new OharaConfigException("the key [" + this.key + "] is required");

      // each type checking
      switch (valueType) {
        case BOOLEAN:
          if (trueValue instanceof Boolean) return;
          // we implement our logic here since
          // boolean type should only accept two possible values: "true" or "false"
          if (!String.valueOf(trueValue).equalsIgnoreCase("true")
              && !String.valueOf(trueValue).equalsIgnoreCase("false"))
            throw new OharaConfigException(
                this.key,
                trueValue,
                "The value should equals to case-insensitive string value of 'true' or 'false'");
          break;
        case STRING:
          try {
            String.valueOf(trueValue);
          } catch (Exception e) {
            throw new OharaConfigException(this.key, trueValue, e.getMessage());
          }
          break;
        case SHORT:
          if (trueValue instanceof Short) return;
          try {
            Short.parseShort(String.valueOf(trueValue));
          } catch (NumberFormatException e) {
            throw new OharaConfigException(this.key, trueValue, e.getMessage());
          }
          break;
        case INT:
          if (trueValue instanceof Integer) return;
          try {
            Integer.parseInt(String.valueOf(trueValue));
          } catch (NumberFormatException e) {
            throw new OharaConfigException(this.key, trueValue, e.getMessage());
          }
          break;
        case LONG:
          if (trueValue instanceof Long) return;
          try {
            Long.parseLong(String.valueOf(trueValue));
          } catch (NumberFormatException e) {
            throw new OharaConfigException(this.key, trueValue, e.getMessage());
          }
          break;
        case DOUBLE:
          if (trueValue instanceof Double) return;
          try {
            Double.parseDouble(String.valueOf(trueValue));
          } catch (NumberFormatException e) {
            throw new OharaConfigException(this.key, trueValue, e.getMessage());
          }
          break;
        case ARRAY:
          try {
            // Determine whether the value is JSON array or not.
            // Note: we hard-code the "kafka list format" to json array here for checking,
            // but it should be implemented a better way (add a converter interface for example)
            JsonUtils.toObject(String.valueOf(trueValue), new TypeReference<List<String>>() {});
          } catch (Exception e) {
            // TODO refactor this in #2400
            try {
              String.valueOf(value).split(",");
            } catch (Exception ex) {
              throw new OharaConfigException("value not match the array string, actual: " + value);
            }
          }
          break;
        case CLASS:
          // TODO: implement the class checking
          break;
        case PASSWORD:
          try {
            String.valueOf(trueValue);
          } catch (Exception e) {
            throw new OharaConfigException(this.key, trueValue, e.getMessage());
          }
          break;
        case JDBC_TABLE:
          // TODO: implement the jdbc table checking
          break;
        case TABLE:
          try {
            PropGroups propGroups = PropGroups.ofJson(String.valueOf(trueValue));
            if (tableKeys.isEmpty()) return;
            if (propGroups.isEmpty()) throw new IllegalArgumentException("row is empty");
            propGroups
                .raw()
                .forEach(
                    row ->
                        tableKeys.forEach(
                            tableKey -> {
                              if (!row.containsKey(tableKey))
                                throw new IllegalArgumentException(
                                    "table key:"
                                        + tableKey
                                        + " does not exist in row:"
                                        + String.join(",", row.keySet()));
                            }));

          } catch (Exception e) {
            throw new OharaConfigException(
                this.key, trueValue, "can't be converted to PropGroups type");
          }
          break;
        case DURATION:
          try {
            CommonUtils.toDuration(String.valueOf(trueValue));
          } catch (Exception e) {
            throw new OharaConfigException(
                this.key, trueValue, "can't be converted to Duration type");
          }
          break;
        case BINDING_PORT:
        case PORT:
          try {
            int port = Integer.valueOf(String.valueOf(trueValue));
            if (!CommonUtils.isConnectionPort(port))
              throw new OharaConfigException(
                  "the legal range for port is [1, 65535], but actual port is " + port);
            if (valueType == Type.BINDING_PORT) {
              // tries to bind the port :)
              try (ServerSocket socket = new ServerSocket(port)) {
                if (port != socket.getLocalPort())
                  throw new OharaConfigException(
                      "the port:" + port + " is not available in host:" + CommonUtils.hostname());
              }
            }
          } catch (Exception e) {
            throw new OharaConfigException(this.key, trueValue, e.getMessage());
          }
          break;
        case JAR_KEY:
          try {
            ObjectKey.toObjectKey(String.valueOf(trueValue));
          } catch (Exception e) {
            throw new OharaConfigException(this.key, trueValue, e.getMessage());
          }
          break;
        case TOPIC_KEYS:
          try {
            if (TopicKey.toTopicKeys(String.valueOf(trueValue)).isEmpty())
              throw new OharaConfigException("TOPIC_KEYS can't be empty!!!");
          } catch (Exception e) {
            throw new OharaConfigException(this.key, trueValue, e.getMessage());
          }
          break;
        case CONNECTOR_KEY:
          try {
            // try parse the json string to Connector Key
            ConnectorKey.toConnectorKey(String.valueOf(trueValue));
          } catch (Exception e) {
            throw new OharaConfigException(this.key, trueValue, e.getMessage());
          }
          break;
        case TAGS:
          try {
            // Determine whether the value is JSON object or not (We assume the "tags" field is an
            // json object)
            JsonUtils.toObject(String.valueOf(trueValue), new TypeReference<Object>() {});
          } catch (Exception e) {
            throw new OharaConfigException(this.key, trueValue, e.getMessage());
          }
          break;
        default:
          // do nothing
          break;
      }
    };
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
  @JsonProperty(GROUP_KEY)
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
  public Type valueType() {
    return valueType;
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

  @JsonProperty(REFERENCE_KEY)
  public Reference reference() {
    return reference;
  }

  @JsonProperty(TABLE_KEYS_KEY)
  public List<String> tableKeys() {
    return new ArrayList<>(tableKeys);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof SettingDef) return toJsonString().equals(((SettingDef) obj).toJsonString());
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

  public static Builder builder(SettingDef definition) {
    return new Builder(definition);
  }

  public static class Builder implements com.island.ohara.common.pattern.Builder<SettingDef> {
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

    private Builder(SettingDef definition) {
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

    @Optional("default value is false")
    public Builder internal() {
      this.internal = true;
      return this;
    }

    /**
     * this is a specific field fot Type.TABLE. It defines the keys' name for table.
     *
     * @param tableKeys key name of table
     * @return this builder
     */
    @Optional("default value is empty")
    public Builder tableKeys(List<String> tableKeys) {
      this.tableKeys = new ArrayList<>(CommonUtils.requireNonEmpty(tableKeys));
      return this;
    }

    public Builder key(String key) {
      this.key = CommonUtils.requireNonEmpty(key);
      return this;
    }

    @Optional("default type is STRING")
    public Builder valueType(Type valueType) {
      this.valueType = Objects.requireNonNull(valueType);
      return this;
    }

    @Optional("default is \"required!\" value")
    public Builder optional(boolean defaultValue) {
      return optional(String.valueOf(defaultValue));
    }

    @Optional("default is \"required!\" value")
    public Builder optional(short defaultValue) {
      return optional(String.valueOf(defaultValue));
    }

    @Optional("default is \"required!\" value")
    public Builder optional(int defaultValue) {
      return optional(String.valueOf(defaultValue));
    }

    @Optional("default is \"required!\" value")
    public Builder optional(long defaultValue) {
      return optional(String.valueOf(defaultValue));
    }

    @Optional("default is \"required!\" value")
    public Builder optional(double defaultValue) {
      return optional(String.valueOf(defaultValue));
    }

    @Optional("default is \"required!\" value")
    public Builder optional(Duration defaultValue) {
      return optional(defaultValue.toString());
    }

    @Optional("default is \"required!\" value")
    public Builder optional(ObjectKey key) {
      return optional(ObjectKey.toJsonString(key));
    }

    @Optional("default is \"required!\" value")
    public Builder optional(TopicKey key) {
      return optional(TopicKey.toJsonString(key));
    }

    @Optional("default is \"required!\" value")
    public Builder optional(ConnectorKey key) {
      return optional(ConnectorKey.toJsonString(key));
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
     *
     * @param reference the reference type
     * @return this builder
     */
    @Optional("Using in Ohara Manager. Default is None")
    public Builder reference(Reference reference) {
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

    @Optional("default setting is modifiable")
    public Builder readonly() {
      this.editable = false;
      return this;
    }

    @Optional("default value is equal to key")
    public Builder displayName(String displayName) {
      this.displayName = CommonUtils.requireNonEmpty(displayName);
      return this;
    }

    @Override
    public SettingDef build() {
      return new SettingDef(
          displayName,
          group,
          orderInGroup,
          editable,
          key,
          valueType,
          required,
          defaultValue,
          documentation,
          reference,
          internal,
          tableKeys);
    }
  }
}
