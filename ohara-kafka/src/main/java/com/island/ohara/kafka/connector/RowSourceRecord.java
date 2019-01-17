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

package com.island.ohara.kafka.connector;

import com.island.ohara.common.data.Row;
import com.island.ohara.common.data.Serializer;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** A wrap to SourceRecord. Currently, only value schema and value are changed. */
public class RowSourceRecord {
  private final Map<String, ?> sourcePartition;
  private final Map<String, ?> sourceOffset;
  private final String topic;
  private final Optional<Integer> partition;
  private final Row row;
  private final Optional<Long> timestamp;

  public RowSourceRecord(
      Map<String, ?> sourcePartition,
      Map<String, ?> sourceOffset,
      String topic,
      Optional<Integer> partition,
      Row row,
      Optional<Long> timestamp) {
    this.sourcePartition = sourcePartition;
    this.sourceOffset = sourceOffset;
    this.topic = topic;
    this.partition = partition;
    this.row = row;
    this.timestamp = timestamp;
  }

  public Map<String, ?> sourcePartition() {
    return sourcePartition;
  }

  public Map<String, ?> sourceOffset() {
    return sourceOffset;
  }

  public String topic() {
    return topic;
  }

  public Optional<Integer> partition() {
    return partition;
  }

  public Row row() {
    return row;
  }

  public Optional<Long> timestamp() {
    return timestamp;
  }

  /**
   * TODO: we don't discuss the key from row yet...
   *
   * @return byte array from row
   */
  byte[] key() {
    return Serializer.ROW.to(row);
  }

  public static RowSourceRecord of(String topic, Row row) {
    return builder().row(row).build(topic);
  }

  public static RowSourceRecordBuilder builder() {
    return new RowSourceRecordBuilder();
  }

  public static class RowSourceRecordBuilder {
    private Map<String, ?> sourcePartition = Collections.emptyMap();
    private Map<String, ?> sourceOffset = Collections.emptyMap();
    private Optional<Integer> partition = Optional.empty();
    private Row row = Row.EMPTY;
    private Optional<Long> timestamp = Optional.empty();

    public RowSourceRecordBuilder sourcePartition(Map<String, ?> sourcePartition) {
      this.sourcePartition = Objects.requireNonNull(sourcePartition);
      return this;
    }

    public RowSourceRecordBuilder sourceOffset(Map<String, ?> sourceOffset) {
      this.sourceOffset = Objects.requireNonNull(sourceOffset);
      return this;
    }

    public RowSourceRecordBuilder _partition(int partition) {
      this.partition = Optional.of(partition);
      return this;
    }

    /** this is a helper method for RowSourceTask */
    public RowSourceRecordBuilder _partition(Optional<Integer> partition) {
      this.partition = partition;
      return this;
    }

    public RowSourceRecordBuilder row(Row row) {
      this.row = Objects.requireNonNull(row);
      return this;
    }

    public RowSourceRecordBuilder _timestamp(long timestamp) {
      this.timestamp = Optional.of(timestamp);
      return this;
    }

    /** this is a helper method for RowSourceTask */
    public RowSourceRecordBuilder _timestamp(Optional<Long> timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    public RowSourceRecord build(String topic) {
      return new RowSourceRecord(
          sourcePartition,
          sourceOffset,
          Objects.requireNonNull(topic),
          partition,
          Objects.requireNonNull(row),
          timestamp);
    }
  }
}
