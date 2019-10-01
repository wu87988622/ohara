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

import com.fasterxml.jackson.core.type.TypeReference;
import com.island.ohara.common.json.JsonUtils;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This key represents a unique object stored in Ohara Configurator. This class is moved from
 * ohara-client to ohara-kafka on account of ConnectorFormatter. Otherwise, the suitable place is
 * ohara-client ...
 */
public interface ObjectKey extends Comparable<ObjectKey> {

  @Override
  default int compareTo(ObjectKey another) {
    int rval = group().compareTo(another.group());
    if (rval != 0) return rval;
    return name().compareTo(another.name());
  }

  /**
   * @param group group
   * @param name name
   * @return a serializable instance
   */
  static ObjectKey of(String group, String name) {
    return new KeyImpl(group, name);
  }

  static String toJsonString(ObjectKey key) {
    return new KeyImpl(key.group(), key.name()).toJsonString();
  }

  static String toJsonString(Collection<? extends ObjectKey> keys) {
    return JsonUtils.toString(
        keys.stream()
            .map(
                key -> {
                  if (key instanceof KeyImpl) return (KeyImpl) key;
                  else return new KeyImpl(key.group(), key.name());
                })
            .collect(Collectors.toList()));
  }
  /**
   * parse input json and then generate a ObjectKey instance.
   *
   * <p>Noted: the parser of scala version is powerful to accept multiples format of requests. By
   * contrast, this json parser is too poor to accept following formats: { "name": "n" }
   *
   * <p>and
   *
   * <p>"n"
   *
   * <p>however, it is ok to java version as this parser is used internally. The data transferred
   * internally is normalized to a standard format: { "group": "n", "name": "n" } and hence we don't
   * worry about taking other "supported" formats for java code.
   *
   * @param json json representation
   * @return a serializable instance
   */
  static ObjectKey toObjectKey(String json) {
    return JsonUtils.toObject(json, new TypeReference<KeyImpl>() {});
  }

  /**
   * parse input json and then generate a ObjectKey instance.
   *
   * <p>Noted: the parser of scala version is powerful to accept multiples format of requests. By
   * contrast, this json parser is too poor to accept following formats: { "name": "n" }
   *
   * <p>and
   *
   * <p>"n"
   *
   * <p>however, it is ok to java version as this parser is used internally. The data transferred
   * internally is normalized to a standard format: { "group": "n", "name": "n" } and hence we don't
   * worry about taking other "supported" formats for java code.
   *
   * @param json json representation
   * @return a serializable instance
   */
  static List<ObjectKey> toObjectKeys(String json) {
    return JsonUtils.toObject(json, new TypeReference<List<KeyImpl>>() {}).stream()
        .map(key -> (ObjectKey) key)
        .collect(Collectors.toList());
  }

  /** @return the group of object */
  String group();

  /** @return the name of object */
  String name();
}
