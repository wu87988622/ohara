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

package com.island.ohara.kafka;

import java.util.Objects;

/**
 * get TopicOption from kafka client
 *
 * @see KafkaClient;
 */
public class TopicOption {
  private final String key;
  private final String value;
  private final Boolean isDefault;
  private final Boolean isSensitive;
  private final Boolean isReadOnly;

  public TopicOption(
      String key, String value, Boolean isDefault, Boolean isSensitive, Boolean isReadOnly) {
    this.key = key;
    this.value = value;
    this.isDefault = isDefault;
    this.isSensitive = isSensitive;
    this.isReadOnly = isReadOnly;
  }

  public String key() {
    return key;
  }

  public String value() {
    return value;
  }

  public Boolean isDefault() {
    return isDefault;
  }

  public Boolean isSensitive() {
    return isSensitive;
  }

  public Boolean isReadOnly() {
    return isReadOnly;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TopicOption that = (TopicOption) o;
    return Objects.equals(key, that.key)
        && Objects.equals(value, that.value)
        && Objects.equals(isDefault, that.isDefault)
        && Objects.equals(isSensitive, that.isSensitive)
        && Objects.equals(isReadOnly, that.isReadOnly);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, value, isDefault, isSensitive, isReadOnly);
  }
}
