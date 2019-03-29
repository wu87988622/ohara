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

import com.fasterxml.jackson.core.type.TypeReference;
import com.island.ohara.common.data.Column;
import com.island.ohara.common.data.DataType;
import com.island.ohara.common.util.CommonUtils;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class PropGroups implements Iterable<Map<String, String>> {
  public static PropGroups of(List<Map<String, String>> values) {
    return new PropGroups(values);
  }

  public static PropGroups ofJson(String json) {
    return of(JsonUtils.toObject(json, new TypeReference<List<Map<String, String>>>() {}));
  }

  public static PropGroups ofColumns(List<Column> columns) {
    return of(columns.stream().map(PropGroups::toPropGroup).collect(Collectors.toList()));
  }

  private final List<Map<String, String>> values;

  private PropGroups(List<Map<String, String>> values) {
    this.values =
        Collections.unmodifiableList(
            values.stream()
                .filter(s -> !s.isEmpty())
                .map(v -> Collections.unmodifiableMap(new HashMap<>(v)))
                .collect(Collectors.toList()));
    this.values.forEach(
        props ->
            props.forEach(
                (k, v) -> {
                  CommonUtils.requireNonEmpty(k);
                  CommonUtils.requireNonEmpty(v);
                }));
  }

  public List<Column> toColumns() {
    return values.stream().map(PropGroups::toColumn).collect(Collectors.toList());
  }

  public Map<String, String> propGroup(int index) {
    return new HashMap<>(values.get(index));
  }

  public boolean isEmpty() {
    return values.isEmpty();
  }

  public int size() {
    return values.size();
  }

  public int numberOfElements() {
    return values.stream().mapToInt(Map::size).sum();
  }

  /** @return a unmodifiable list of raw data */
  public List<Map<String, String>> raw() {
    // the values is unmodifiable already.
    return values;
  }

  public String toJsonString() {
    return JsonUtils.toString(values);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof PropGroups) return ((PropGroups) obj).toJsonString().equals(toJsonString());
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

  private static Column toColumn(Map<String, String> propGroup) {
    return Column.builder()
        .order(Integer.valueOf(propGroup.get(SettingDefinition.ORDER_KEY)))
        .name(propGroup.get(SettingDefinition.COLUMN_NAME_KEY))
        .newName(propGroup.get(SettingDefinition.COLUMN_NEW_NAME_KEY))
        .dataType(DataType.valueOf(propGroup.get(SettingDefinition.COLUMN_DATA_TYPE_KEY)))
        .build();
  }

  private static Map<String, String> toPropGroup(Column column) {
    Map<String, String> propGroup = new HashMap<>();
    propGroup.put(SettingDefinition.ORDER_KEY, String.valueOf(column.order()));
    propGroup.put(SettingDefinition.COLUMN_NAME_KEY, column.name());
    propGroup.put(SettingDefinition.COLUMN_NEW_NAME_KEY, column.newName());
    propGroup.put(SettingDefinition.COLUMN_DATA_TYPE_KEY, column.dataType().name());
    return propGroup;
  }

  @Override
  public Iterator<Map<String, String>> iterator() {
    return values.iterator();
  }
}
