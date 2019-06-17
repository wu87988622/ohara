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

package com.island.ohara.common.data;

import com.island.ohara.common.annotations.Optional;
import com.island.ohara.common.util.CommonUtils;
import java.io.Serializable;
import java.util.Objects;

/** implements Serializable ,because akka unmashaller throws java.io.NotSerializableException */
public final class Column extends Data implements Serializable {

  private static final long serialVersionUID = 1L;

  private final String name;
  private final String newName;
  private final DataType dataType;
  private final int order;

  private Column(String name, String newName, DataType dataType, int order) {
    this.name = CommonUtils.requireNonEmpty(name);
    this.newName = CommonUtils.requireNonEmpty(newName);
    this.dataType = Objects.requireNonNull(dataType);
    this.order = CommonUtils.requireNonNegativeInt(order);
  }

  public String name() {
    return name;
  }

  public String newName() {
    return newName;
  }

  public DataType dataType() {
    return dataType;
  }

  public int order() {
    return order;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String name;
    private String newName;
    private DataType dataType;
    private int order;

    private Builder() {}

    public Builder name(String name) {
      this.name = CommonUtils.requireNonEmpty(name);
      // default the new name is equal to name
      this.newName = name;
      return this;
    }

    @Optional("default is same to $name")
    public Builder newName(String newName) {
      this.newName = CommonUtils.requireNonEmpty(newName);
      return this;
    }

    public Builder dataType(DataType dataType) {
      this.dataType = Objects.requireNonNull(dataType);
      return this;
    }

    public Builder order(int order) {
      this.order = CommonUtils.requireNonNegativeInt(order);
      return this;
    }

    public Column build() {
      return new Column(name, newName, dataType, order);
    }
  }
}
