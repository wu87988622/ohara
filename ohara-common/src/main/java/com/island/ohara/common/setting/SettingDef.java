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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * This class is the base class to define configuration for ohara object.
 *
 * <p>SettingDef is stored by Configurator Store now, and the serialization is based on java
 * serializable. Hence, we add this Serializable interface here.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
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
    ZOOKEEPER_CLUSTER,
    BROKER_CLUSTER,
    WORKER_CLUSTER,
    FILE
  }

  // -------------------------------[value required]-------------------------------//
  public enum Necessary {
    REQUIRED,
    OPTIONAL,
    OPTIONAL_WITH_DEFAULT,
    OPTIONAL_WITH_RANDOM_DEFAULT
  }

  // -------------------------------[Check rule]-------------------------------//
  public enum CheckRule {
    NONE,
    PERMISSIVE,
    ENFORCING
  }

  // -------------------------------[type]-------------------------------//
  public enum Type {
    BOOLEAN,
    STRING,
    POSITIVE_SHORT,
    SHORT,
    POSITIVE_INT,
    INT,
    POSITIVE_LONG,
    LONG,
    POSITIVE_DOUBLE,
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
    /** { "group": "g", "name":" n" } */
    OBJECT_KEY,
    /** [ { "group": "g", "name":" n" } ] */
    OBJECT_KEYS,
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
  private static final String NECESSARY_KEY = "necessary";
  private static final String DEFAULT_VALUE_KEY = "defaultValue";
  private static final String DOCUMENTATION_KEY = "documentation";
  private static final String INTERNAL_KEY = "internal";
  private static final String TABLE_KEYS_KEY = "tableKeys";
  // exposed to TableColumn
  static final String RECOMMENDED_VALUES_KEY = "recommendedValues";
  private static final String BLACKLIST_KEY = "blacklist";

  public static SettingDef ofJson(String json) {
    return JsonUtils.toObject(json, new TypeReference<SettingDef>() {});
  }

  private final String displayName;
  private final String group;
  private final int orderInGroup;
  private final boolean editable;
  private final String key;
  private final Type valueType;
  @Nullable private final Object defaultValue;
  private final Necessary necessary;
  private final String documentation;
  private final Reference reference;
  private final boolean internal;
  private final List<TableColumn> tableKeys;
  private final Set<String> recommendedValues;
  private final Set<String> blacklist;

  @JsonCreator
  private SettingDef(
      @JsonProperty(DISPLAY_NAME_KEY) String displayName,
      @JsonProperty(GROUP_KEY) String group,
      @JsonProperty(ORDER_IN_GROUP_KEY) int orderInGroup,
      @JsonProperty(EDITABLE_KEY) boolean editable,
      @JsonProperty(KEY_KEY) String key,
      @JsonProperty(VALUE_TYPE_KEY) Type valueType,
      @JsonProperty(NECESSARY_KEY) Necessary necessary,
      @Nullable @JsonProperty(DEFAULT_VALUE_KEY) Object defaultValue,
      @JsonProperty(DOCUMENTATION_KEY) String documentation,
      @Nullable @JsonProperty(REFERENCE_KEY) Reference reference,
      @JsonProperty(INTERNAL_KEY) boolean internal,
      @JsonProperty(TABLE_KEYS_KEY) List<TableColumn> tableKeys,
      @JsonProperty(RECOMMENDED_VALUES_KEY) Set<String> recommendedValues,
      @JsonProperty(BLACKLIST_KEY) Set<String> blacklist) {
    this.group = CommonUtils.requireNonEmpty(group);
    this.orderInGroup = orderInGroup;
    this.editable = editable;
    this.key = CommonUtils.requireNonEmpty(key);
    if (this.key.contains("__"))
      throw new IllegalArgumentException(
          "the __ is keyword so it is illegal word to definition key");
    this.valueType = Objects.requireNonNull(valueType);
    this.necessary = necessary;
    if (valueType == Type.SHORT && defaultValue instanceof Integer) {
      // jackson convert the number to int by default so we have to cast it again.
      this.defaultValue = (short) ((int) defaultValue);
    } else this.defaultValue = defaultValue;
    this.documentation = CommonUtils.requireNonEmpty(documentation);
    this.reference = Objects.requireNonNull(reference);
    this.internal = internal;
    this.tableKeys = Objects.requireNonNull(tableKeys);
    // It is legal to ignore the display name.
    // However, we all hate null so we set the default value equal to key.
    this.displayName = CommonUtils.isEmpty(displayName) ? this.key : displayName;
    this.recommendedValues = Objects.requireNonNull(recommendedValues);
    this.blacklist = Objects.requireNonNull(blacklist);
  }

  /**
   * Generate official checker according to input type.
   *
   * @return checker
   */
  public Consumer<Object> checker() {
    return (Object value) -> {
      // we don't check the optional key with null value
      if (this.necessary != Necessary.REQUIRED && this.defaultValue == null && value == null)
        return;

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
        case POSITIVE_SHORT:
        case SHORT:
          try {
            int v;
            if (trueValue instanceof Short) v = (short) trueValue;
            else v = Short.parseShort(String.valueOf(trueValue));
            if (valueType == SettingDef.Type.POSITIVE_SHORT && v <= 0)
              throw new OharaConfigException(
                  this.key, trueValue, "the value must be bigger than zero but actual:" + v);
          } catch (NumberFormatException e) {
            throw new OharaConfigException(this.key, trueValue, e.getMessage());
          }
          break;
        case POSITIVE_INT:
        case INT:
          try {
            int v;
            if (trueValue instanceof Integer) v = (int) trueValue;
            else v = Integer.parseInt(String.valueOf(trueValue));
            if (valueType == SettingDef.Type.POSITIVE_INT && v <= 0)
              throw new OharaConfigException(
                  this.key, trueValue, "the value must be bigger than zero but actual:" + v);
          } catch (NumberFormatException e) {
            throw new OharaConfigException(this.key, trueValue, e.getMessage());
          }
          break;
        case POSITIVE_LONG:
        case LONG:
          try {
            long v;
            if (trueValue instanceof Long) v = (long) trueValue;
            else v = Long.parseLong(String.valueOf(trueValue));
            if (valueType == SettingDef.Type.POSITIVE_LONG && v <= 0)
              throw new OharaConfigException(
                  this.key, trueValue, "the value must be bigger than zero but actual:" + v);
          } catch (NumberFormatException e) {
            throw new OharaConfigException(this.key, trueValue, e.getMessage());
          }
          break;
        case POSITIVE_DOUBLE:
        case DOUBLE:
          try {
            double v;
            if (trueValue instanceof Double) v = (double) trueValue;
            else v = Double.parseDouble(String.valueOf(trueValue));
            if (valueType == SettingDef.Type.POSITIVE_DOUBLE && v <= 0)
              throw new OharaConfigException(
                  this.key, trueValue, "the value must be bigger than zero but actual:" + v);
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
            PropGroup propGroup = PropGroup.ofJson(String.valueOf(trueValue));
            if (tableKeys.isEmpty()) return;
            if (propGroup.isEmpty()) throw new IllegalArgumentException("row is empty");
            propGroup
                .raw()
                .forEach(
                    row -> {
                      if (!tableKeys.isEmpty()) {
                        Set<String> expectedColumnNames =
                            tableKeys.stream().map(TableColumn::name).collect(Collectors.toSet());
                        Set<String> actualColumnName = row.keySet();
                        if (!actualColumnName.equals(expectedColumnNames)) {
                          throw new IllegalArgumentException(
                              "expected column names:"
                                  + String.join(",", expectedColumnNames)
                                  + ", actual:"
                                  + String.join(",", actualColumnName));
                        }
                      }
                    });

          } catch (Exception e) {
            throw new OharaConfigException(
                this.key, trueValue, "can't be converted to PropGroup type");
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
        case OBJECT_KEYS:
          try {
            if (TopicKey.toTopicKeys(String.valueOf(trueValue)).isEmpty())
              throw new OharaConfigException("OBJECT_KEYS can't be empty!!!");
          } catch (Exception e) {
            throw new OharaConfigException(this.key, trueValue, e.getMessage());
          }
          break;
        case OBJECT_KEY:
          try {
            // try parse the json string to Connector Key
            ObjectKey.toObjectKey(String.valueOf(trueValue));
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

  @JsonProperty(NECESSARY_KEY)
  public Necessary necessary() {
    return necessary;
  }

  /**
   * get and convert the default value to boolean type
   *
   * @return default value in boolean type
   */
  public boolean defaultBoolean() {
    if (valueType == Type.BOOLEAN) return (boolean) Objects.requireNonNull(defaultValue);
    throw new IllegalStateException("expected type: boolean, but actual:" + valueType);
  }

  /**
   * get and convert the default value to short type
   *
   * @return default value in short type
   */
  public short defaultShort() {
    if (valueType == Type.SHORT) return (short) Objects.requireNonNull(defaultValue);
    throw new IllegalStateException("expected type: short, but actual:" + valueType);
  }

  /**
   * get and convert the default value to int type
   *
   * @return default value in int type
   */
  public int defaultInt() {
    if (valueType == Type.INT) return (int) Objects.requireNonNull(defaultValue);
    throw new IllegalStateException("expected type: int, but actual:" + valueType);
  }

  /**
   * get and convert the default value to long type
   *
   * @return default value in long type
   */
  public long defaultLong() {
    if (valueType == Type.LONG) return (long) Objects.requireNonNull(defaultValue);
    throw new IllegalStateException("expected type: long, but actual:" + valueType);
  }

  /**
   * get and convert the default value to double type
   *
   * @return default value in double type
   */
  public double defaultDouble() {
    if (valueType == Type.DOUBLE) return (double) Objects.requireNonNull(defaultValue);
    throw new IllegalStateException("expected type: double, but actual:" + valueType);
  }

  /**
   * get and convert the default value to String type
   *
   * @return default value in String type
   */
  public String defaultString() {
    switch (valueType) {
      case STRING:
      case CLASS:
      case PASSWORD:
      case JDBC_TABLE:
        return (String) Objects.requireNonNull(defaultValue);
      default:
        throw new IllegalStateException("expected type: String, but actual:" + valueType);
    }
  }

  /**
   * get and convert the default value to Duration type
   *
   * @return default value in Duration type
   */
  public Duration defaultDuration() {
    if (valueType == Type.DURATION)
      return CommonUtils.toDuration((String) Objects.requireNonNull(defaultValue));
    throw new IllegalStateException("expected type: Duration, but actual:" + valueType);
  }

  /** @return true if there is a default value. otherwise, false */
  public boolean hasDefault() {
    return defaultValue != null;
  }

  /**
   * Normally, you should not use this method since it does not convert the value according to type.
   * the main purpose of this method is exposed to jackson to render the json string.
   *
   * @return origin type (object type)
   */
  @Nullable
  @JsonProperty(DEFAULT_VALUE_KEY)
  public Object defaultValue() {
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
  public List<TableColumn> tableKeys() {
    return Collections.unmodifiableList(tableKeys);
  }

  @JsonProperty(RECOMMENDED_VALUES_KEY)
  public Set<String> recommendedValues() {
    return Collections.unmodifiableSet(recommendedValues);
  }

  @JsonProperty(BLACKLIST_KEY)
  public Set<String> blacklist() {
    return Collections.unmodifiableSet(blacklist);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof SettingDef) {
      SettingDef another = (SettingDef) obj;
      return Objects.equals(displayName, another.displayName)
          && Objects.equals(group, another.group)
          && Objects.equals(orderInGroup, another.orderInGroup)
          && Objects.equals(editable, another.editable)
          && Objects.equals(key, another.key)
          && Objects.equals(valueType, another.valueType)
          && Objects.equals(necessary, another.necessary)
          && Objects.equals(defaultValue, another.defaultValue)
          && Objects.equals(documentation, another.documentation)
          && Objects.equals(reference, another.reference)
          && Objects.equals(internal, another.internal)
          && Objects.equals(tableKeys, another.tableKeys)
          && Objects.equals(recommendedValues, another.recommendedValues)
          && Objects.equals(blacklist, another.blacklist);
    }
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

  public static class Builder implements com.island.ohara.common.pattern.Builder<SettingDef> {
    private String displayName;
    private String group = COMMON_GROUP;
    private int orderInGroup = -1;
    private boolean editable = true;
    private String key;
    private Type valueType = null;
    private Necessary necessary = null;
    @Nullable private Object defaultValue = null;
    private String documentation = "this is no documentation for this setting";
    private Reference reference = Reference.NONE;
    private boolean internal = false;
    private List<TableColumn> tableKeys = Collections.emptyList();
    private Set<String> recommendedValues = Collections.emptySet();
    private Set<String> blacklist = Collections.emptySet();

    private Builder() {}

    @Optional("default value is false")
    public Builder internal() {
      this.internal = true;
      return this;
    }

    /**
     * noted that the legal chars are digits (0-9), letters (a-z|A-Z), -, and .
     *
     * @param key key
     * @return this builder
     */
    public Builder key(String key) {
      this.key = CommonUtils.requireNonEmpty(key);
      if (!key.matches("[a-zA-Z0-9\\._\\-]+"))
        throw new IllegalArgumentException("the legal char is [a-zA-Z0-9\\._\\-]+");
      return this;
    }

    /** check and set all related fields at once. */
    private Builder checkAndSet(Type valueType, Necessary necessary, Object defaultValue) {
      if (defaultValue != null) {
        switch (valueType) {
          case ARRAY:
          case TABLE:
          case OBJECT_KEY:
          case OBJECT_KEYS:
          case TAGS:
            throw new IllegalArgumentException("type:" + valueType + " can't have default value");
          default:
            break;
        }
      }
      if (this.valueType != null)
        throw new IllegalArgumentException(
            "type is defined to " + this.valueType + ", new one:" + valueType);
      this.valueType = Objects.requireNonNull(valueType);
      if (this.necessary != null)
        throw new IllegalArgumentException(
            "necessary is defined to " + this.necessary + ", new one:" + necessary);
      this.necessary = Objects.requireNonNull(necessary);
      if (this.defaultValue != null)
        throw new IllegalArgumentException(
            "defaultValue is defined to " + this.defaultValue + ", new one:" + defaultValue);
      this.defaultValue = defaultValue;
      return this;
    }

    /**
     * set the type. Noted: the following types are filled with default empty. 1) {@link
     * SettingDef.Type#ARRAY} 2) {@link SettingDef.Type#TAGS} 3) {@link SettingDef.Type#TABLE} 4)
     * {@link SettingDef.Type#OBJECT_KEYS}
     *
     * @param valueType value type
     * @return this builder
     */
    public Builder required(Type valueType) {
      return checkAndSet(valueType, Necessary.REQUIRED, null);
    }

    /**
     * set the type and announce the empty/null value is legal
     *
     * @param valueType value type
     * @return this builder
     */
    public Builder optional(Type valueType) {
      return checkAndSet(valueType, Necessary.OPTIONAL, null);
    }

    /**
     * set the value type to TABLE and give a rule to table schema.
     *
     * @param tableKeys key name of table
     * @return this builder
     */
    public Builder optional(List<TableColumn> tableKeys) {
      this.tableKeys = new ArrayList<>(CommonUtils.requireNonEmpty(tableKeys));
      return checkAndSet(Type.TABLE, Necessary.OPTIONAL, null);
    }

    /**
     * set the string type and this definition generate random default value if the value is not
     * defined.
     *
     * @return this builder
     */
    public Builder stringWithRandomDefault() {
      return checkAndSet(Type.STRING, Necessary.OPTIONAL_WITH_RANDOM_DEFAULT, null);
    }

    /**
     * set the binding port type and this definition generate random default value if the value is
     * not defined.
     *
     * @return this builder
     */
    public Builder bindingPortWithRandomDefault() {
      return checkAndSet(Type.BINDING_PORT, Necessary.OPTIONAL_WITH_RANDOM_DEFAULT, null);
    }

    /**
     * set the type to boolean and add the default value.
     *
     * @param defaultValue the default boolean value
     * @return builder
     */
    public Builder optional(boolean defaultValue) {
      return checkAndSet(Type.BOOLEAN, Necessary.OPTIONAL_WITH_DEFAULT, defaultValue);
    }

    /**
     * set the type to short and add the default value.
     *
     * @param defaultValue the default short value
     * @return builder
     */
    public Builder optional(short defaultValue) {
      return checkAndSet(Type.SHORT, Necessary.OPTIONAL_WITH_DEFAULT, defaultValue);
    }

    /**
     * set the type to positive short and add the default value.
     *
     * @param defaultValue the default short value
     * @return builder
     */
    public Builder positiveNumber(short defaultValue) {
      return checkAndSet(Type.POSITIVE_SHORT, Necessary.OPTIONAL_WITH_DEFAULT, defaultValue);
    }

    /**
     * set the type to int and add the default value.
     *
     * @param defaultValue the default int value
     * @return builder
     */
    public Builder optional(int defaultValue) {
      return checkAndSet(Type.INT, Necessary.OPTIONAL_WITH_DEFAULT, defaultValue);
    }

    /**
     * set the type to positive int and add the default value.
     *
     * @param defaultValue the default int value
     * @return builder
     */
    public Builder positiveNumber(int defaultValue) {
      return checkAndSet(Type.POSITIVE_INT, Necessary.OPTIONAL_WITH_DEFAULT, defaultValue);
    }

    /**
     * set the type to long and add the default value.
     *
     * @param defaultValue the default long value
     * @return builder
     */
    public Builder optional(long defaultValue) {
      return checkAndSet(Type.LONG, Necessary.OPTIONAL_WITH_DEFAULT, defaultValue);
    }

    /**
     * set the type to positive long and add the default value.
     *
     * @param defaultValue the default long value
     * @return builder
     */
    public Builder positiveNumber(long defaultValue) {
      return checkAndSet(Type.POSITIVE_LONG, Necessary.OPTIONAL_WITH_DEFAULT, defaultValue);
    }

    /**
     * set the type to double and add the default value.
     *
     * @param defaultValue the default double value
     * @return builder
     */
    public Builder optional(double defaultValue) {
      return checkAndSet(Type.DOUBLE, Necessary.OPTIONAL_WITH_DEFAULT, defaultValue);
    }

    /**
     * set the type to positive double and add the default value.
     *
     * @param defaultValue the default double value
     * @return builder
     */
    public Builder positiveNumber(double defaultValue) {
      return checkAndSet(Type.POSITIVE_DOUBLE, Necessary.OPTIONAL_WITH_DEFAULT, defaultValue);
    }

    /**
     * set the type to Duration and add the default value.
     *
     * @param defaultValue the default Duration value
     * @return builder
     */
    public Builder optional(Duration defaultValue) {
      return checkAndSet(Type.DURATION, Necessary.OPTIONAL_WITH_DEFAULT, defaultValue.toString());
    }

    /**
     * set the type to string and add the default value.
     *
     * @param defaultValue the default string value
     * @return builder
     */
    public Builder optional(String defaultValue) {
      return checkAndSet(
          Type.STRING, Necessary.OPTIONAL_WITH_DEFAULT, CommonUtils.requireNonEmpty(defaultValue));
    }

    /**
     * set the type to string and add the recommended values
     *
     * @param defaultValue the default string value
     * @param recommendedValues recommended string value
     * @return builder
     */
    public Builder optional(String defaultValue, Set<String> recommendedValues) {
      this.recommendedValues = Objects.requireNonNull(recommendedValues);
      return checkAndSet(
          Type.STRING, Necessary.OPTIONAL_WITH_DEFAULT, CommonUtils.requireNonEmpty(defaultValue));
    }

    /**
     * set the type to CLASS
     *
     * @param defaultValue the default CLASS value
     * @return builder
     */
    public Builder optionalClassValue(String defaultValue) {
      return checkAndSet(
          Type.CLASS, Necessary.OPTIONAL_WITH_DEFAULT, CommonUtils.requireNonEmpty(defaultValue));
    }

    /**
     * this method includes following settings. 1. set the type to ARRAY 2. disable specific values
     * 3. set the value to be required
     *
     * @param blacklist the values in this list is illegal
     * @return builder
     */
    public Builder blacklist(Set<String> blacklist) {
      this.blacklist = Objects.requireNonNull(blacklist);
      return checkAndSet(Type.ARRAY, Necessary.REQUIRED, null);
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
          valueType == null ? Type.STRING : valueType,
          necessary == null ? Necessary.REQUIRED : necessary,
          defaultValue,
          documentation,
          reference,
          internal,
          tableKeys,
          recommendedValues,
          blacklist);
    }
  }
}
