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

import com.island.ohara.common.annotations.Nullable;
import com.island.ohara.common.util.CommonUtil;
import java.util.Objects;
import java.util.Optional;

/** The description of ohara connector properties. */
public class Definition {
  private final String name;
  private final Type valueType;
  @Nullable private final Object valueDefault;
  private final boolean required;
  private final String documentation;

  private Definition(
      String name, Type valueType, boolean required, Object valueDefault, String documentation) {
    this.name = CommonUtil.requireNonEmpty(name);
    this.valueType = Objects.requireNonNull(valueType);
    this.required = required;
    this.valueDefault = valueDefault;
    this.documentation = CommonUtil.requireNonEmpty(documentation);
  }

  public String name() {
    return name;
  }

  public Type valueType() {
    return valueType;
  }

  public boolean required() {
    return required;
  }

  public Optional<Object> valueDefault() {
    return Optional.ofNullable(valueDefault);
  }

  public String documentation() {
    return documentation;
  }

  @Override
  public String toString() {
    return "name:"
        + name
        + "valueType:"
        + valueType
        + "required:"
        + required
        + "valueDefault:"
        + valueDefault
        + "documentation:"
        + documentation;
  }

  /** the type of configuration. This class is minor of ConfigDef.Type */
  enum Type {
    BOOLEAN,
    STRING,
    SHORT,
    INT,
    LONG,
    DOUBLE,
    LIST,
    CLASS,
    PASSWORD
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private String name;
    private Type valueType;
    private boolean required = true;
    private Object valueDefault = null;
    private String documentation;

    private Builder() {}

    public Builder name(String name) {
      this.name = CommonUtil.requireNonEmpty(name);
      return this;
    }

    public Builder valueType(Type valueType) {
      this.valueType = Objects.requireNonNull(valueType);
      return this;
    }

    @com.island.ohara.common.annotations.Optional("default is \"required!\" value")
    public Builder optional(Object valueDefault) {
      this.required = false;
      this.valueDefault = Objects.requireNonNull(valueDefault);
      return this;
    }

    @com.island.ohara.common.annotations.Optional("default is \"required!\" value")
    public Builder optional() {
      this.required = false;
      this.valueDefault = null;
      return this;
    }

    @com.island.ohara.common.annotations.Optional("default is \"required!\" value")
    public Builder required() {
      this.required = true;
      this.valueDefault = null;
      return this;
    }

    public Builder documentation(String documentation) {
      this.documentation = CommonUtil.requireNonEmpty(documentation);
      return this;
    }

    public Definition build() {
      return new Definition(name, valueType, required, valueDefault, documentation);
    }
  }
}
