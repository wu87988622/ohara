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
 * the key of connector object. It is almost same with {@link ObjectKey} excepting for the method
 * "connectorNameOnKafka". the method is a helper method used to generate the connector name on
 * kafka.
 */
public interface ConnectorKey extends ObjectKey {

  /**
   * @param group group
   * @param name name
   * @return a serializable instance
   */
  static ConnectorKey of(String group, String name) {
    return new KeyImpl(group, name);
  }

  static String toJsonString(ConnectorKey key) {
    return new KeyImpl(key.group(), key.name()).toJsonString();
  }

  /**
   * parse input json and then generate a ConnectorKey instance.
   *
   * @param json json representation
   * @return a serializable instance
   */
  static ConnectorKey ofJsonString(String json) {
    return JsonUtils.toObject(json, new TypeReference<KeyImpl>() {});
  }

  /**
   * generate the connector name for kafka. Noted: kafka connector does not support group so we
   * generate the name composed of group and name.
   *
   * @return connector name for kafka
   */
  default String connectorNameOnKafka() {
    return group() + "-" + name();
  }
}
