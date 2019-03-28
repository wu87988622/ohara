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
import com.island.ohara.common.data.Column;
import com.island.ohara.common.data.DataType;
import com.island.ohara.common.util.CommonUtils;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class PropGroup implements JsonObject {
  static final String ORDER_KEY = "order";
  private static final String PROPS_KEY = "props";
  static final String COLUMN_NAME_KEY = "name";
  static final String COLUMN_NEW_NAME_KEY = "newName";
  static final String COLUMN_DATA_TYPE_KEY = "dataType";

  public static PropGroup of(Column column) {
    Map<String, String> map = new HashMap<>();
    map.put(COLUMN_NAME_KEY, column.name());
    map.put(COLUMN_NEW_NAME_KEY, column.newName());
    map.put(COLUMN_DATA_TYPE_KEY, column.dataType().name());
    return PropGroup.of(column.order(), map);
  }

  public static PropGroup ofJson(String json) {
    return JsonUtils.toObject(json, new TypeReference<PropGroup>() {});
  }

  public static PropGroup of(int order, String key, String value) {
    Map<String, String> map = new HashMap<>();
    map.put(CommonUtils.requireNonEmpty(key), value);
    return new PropGroup(order, map);
  }

  public static PropGroup of(int order, Map<String, String> props) {
    return new PropGroup(order, props);
  }

  private final int order;
  private final Map<String, String> props;

  @JsonCreator
  private PropGroup(
      @JsonProperty(ORDER_KEY) int order, @JsonProperty(PROPS_KEY) Map<String, String> props) {
    this.order = order;
    props.keySet().forEach(CommonUtils::requireNonEmpty);
    this.props =
        new HashMap<>(
            Objects.requireNonNull(
                props.entrySet().stream()
                    .filter(entry -> !CommonUtils.isEmpty(entry.getValue()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))));
  }

  @JsonProperty(ORDER_KEY)
  public int order() {
    return order;
  }

  /** @return the number of props */
  public int size() {
    return props.size();
  }

  public Optional<String> value(String key) {
    return Optional.ofNullable(props.get(key));
  }

  public String require(String key) {
    return value(key)
        .orElseGet(
            () -> {
              throw new NoSuchElementException(key + " doesn't exit");
            });
  }

  public Column toColumn() {
    return Column.builder()
        .name(require(COLUMN_NAME_KEY))
        .newName(require(COLUMN_NEW_NAME_KEY))
        .dataType(DataType.valueOf(require(COLUMN_DATA_TYPE_KEY)))
        .order(order)
        .build();
  }

  @JsonProperty(PROPS_KEY)
  public Map<String, String> props() {
    return Collections.unmodifiableMap(props);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof PropGroup) return toJsonString().equals(((PropGroup) obj).toJsonString());
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
}
