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
import com.island.ohara.common.util.CommonUtils;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class PropGroups {

  public static String toString(List<PropGroup> propGroups) {
    return JsonUtils.toString(propGroups);
  }

  public static List<PropGroup> ofJson(String json) {
    return CommonUtils.isEmpty(json) || json.equalsIgnoreCase("null")
        ? Collections.emptyList()
        : JsonUtils.toObject(json, new TypeReference<List<PropGroup>>() {});
  }

  public static List<PropGroup> of(List<Column> columns) {
    return columns.stream().map(PropGroup::of).collect(Collectors.toList());
  }

  public static List<Column> toColumns(List<PropGroup> propGroups) {
    return propGroups.stream().map(PropGroup::toColumn).collect(Collectors.toList());
  }

  private PropGroups() {}
}
