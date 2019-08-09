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

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

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
    return ObjectKey.toJsonString(key);
  }

  static String toJsonString(Collection<? extends ConnectorKey> key) {
    return ObjectKey.toJsonString(key);
  }

  /**
   * parse input json and then generate a ConnectorKey instance.
   *
   * @param json json representation
   * @return a serializable instance
   */
  static ConnectorKey toConnectorKey(String json) {
    ObjectKey key = ObjectKey.toObjectKey(json);
    return ConnectorKey.of(key.group(), key.name());
  }

  /**
   * parse input json and then generate a ConnectorKey instance.
   *
   * @param json json representation
   * @return a serializable instance
   */
  static List<ConnectorKey> toConnectorKeys(String json) {
    return ObjectKey.toObjectKeys(json).stream()
        .map(key -> ConnectorKey.of(key.group(), key.name()))
        .collect(Collectors.toList());
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
