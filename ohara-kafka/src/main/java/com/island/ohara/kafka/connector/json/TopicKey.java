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
 * the key of topic object. It is almost same with {@link ObjectKey} excepting for the method
 * "topicNameOnKafka". the method is a helper method used to generate the topic name on kafka.
 */
public interface TopicKey extends ObjectKey {

  /**
   * @param group group
   * @param name name
   * @return a serializable instance
   */
  static TopicKey of(String group, String name) {
    return new KeyImpl(group, name);
  }

  static String toJsonString(TopicKey key) {
    return new KeyImpl(key.group(), key.name()).toJsonString();
  }

  /**
   * parse input json and then generate a TopicKey instance.
   *
   * @param json json representation
   * @return a serializable instance
   */
  static TopicKey ofJsonString(String json) {
    return JsonUtils.toObject(json, new TypeReference<KeyImpl>() {});
  }

  /**
   * generate the topic name for kafka. Noted: kafka topic does not support group so we generate the
   * name composed of group and name
   *
   * @return topic name for kafka
   */
  default String topicNameOnKafka() {
    return group() + "-" + name();
  }
}
