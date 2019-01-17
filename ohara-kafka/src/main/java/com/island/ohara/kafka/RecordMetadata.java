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

/**
 * wrap from kafka RecordMetadata;
 *
 * @see org.apache.kafka.clients.producer.RecordMetadata;
 */
public class RecordMetadata {
  private final String topic;
  private final int partition;
  private final long offset;
  private final long timestamp;
  private final int serializedKeySize;
  private final int serializedValueSize;

  public RecordMetadata(
      String topic,
      int partition,
      long offset,
      long timestamp,
      int serializedKeySize,
      int serializedValueSize) {
    this.topic = topic;
    this.partition = partition;
    this.offset = offset;
    this.timestamp = timestamp;
    this.serializedKeySize = serializedKeySize;
    this.serializedValueSize = serializedValueSize;
  }

  public String topic() {
    return topic;
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
