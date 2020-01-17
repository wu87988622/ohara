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

public class RecordMetadata {
  /**
   * convert kafka.RecordMetadata to ohara.RecordMetadata
   *
   * @param metadata kafka.RecordMetadata
   * @return ohara.RecordMetadata
   */
  public static RecordMetadata of(org.apache.kafka.clients.producer.RecordMetadata metadata) {
    return new RecordMetadata(
        metadata.topic(),
        metadata.partition(),
        metadata.offset(),
        metadata.timestamp(),
        metadata.serializedKeySize(),
        metadata.serializedValueSize());
  }

  private final String topicName;
  private final int partition;
  private final long offset;
  private final long timestamp;
  private final int serializedKeySize;
  private final int serializedValueSize;

  private RecordMetadata(
      String topicName,
      int partition,
      long offset,
      long timestamp,
      int serializedKeySize,
      int serializedValueSize) {
    this.topicName = topicName;
    this.partition = partition;
    this.offset = offset;
    this.timestamp = timestamp;
    this.serializedKeySize = serializedKeySize;
    this.serializedValueSize = serializedValueSize;
  }

  public String topicName() {
    return topicName;
  }

  public int partition() {
    return partition;
  }

  public long offset() {
    return offset;
  }

  public long timestamp() {
    return timestamp;
  }

  public int serializedKeySize() {
    return serializedKeySize;
  }

  public int serializedValueSize() {
    return serializedValueSize;
  }
}
