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
import com.island.ohara.common.json.JsonObject;
import com.island.ohara.common.json.JsonUtils;
import com.island.ohara.common.util.CommonUtils;
import java.util.*;

/** This class is used to define the setting of ohara object. */
public class SettingDef implements JsonObject {
  // -------------------------------[reference]-------------------------------//
  public enum Reference {
    NONE,
    TOPIC,
    WORKER_CLUSTER
  }
  // -------------------------------[type]-----------------------------------//
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
    PORT
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

  public static SettingDef ofJson(String json) {
    return JsonUtils.toObject(json, new TypeReference<SettingDef>() {});
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder implements com.island.ohara.common.pattern.Builder<SettingDef> {
    private Reference reference = Reference.NONE;
    private String displayName;
    private String group;
    private int orderInGroup = -1;
    private boolean editable = true;
    private String key;
    private Type valueType = Type.STRING;
    private boolean required = true;
    @Nullable private String defaultValue = null;
    private String documentation = "this is no documentation for this setting";
    private boolean internal = false;
    private List<String> tableKeys = Collections.emptyList();

    private Builder() {}

    @Optional("default value is false")
    public Builder internal() {
      this.internal = true;
      return this;
    }

    /**
     * This property is required by ohara manager. There are some official setting having particular
     * control on UI.
     */
    @Optional("default is None")
    public Builder reference(Reference reference) {
      this.reference = Objects.requireNonNull(reference);
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
      this.tableKeys = CommonUtils.requireNonEmpty(tableKeys);
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
