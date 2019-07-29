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
import com.island.ohara.common.json.JsonUtils;

/**
 * This key represents a unique object stored in Ohara Configurator. This class is moved from
 * ohara-client to ohara-kafka on account of ConnectorFormatter. Otherwise, the suitable place is
 * ohara-client ...
 */
public interface ObjectKey {

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

  /**
   * parse input json and then generate a ObjectKey instance.
   *
   * @param json json representation
   * @return a serializable instance
   */
  static ObjectKey ofJsonString(String json) {
    return JsonUtils.toObject(json, new TypeReference<KeyImpl>() {});
  }

  /** @return the group of object */
  String group();

  /** @return the name of object */
  String name();
}
