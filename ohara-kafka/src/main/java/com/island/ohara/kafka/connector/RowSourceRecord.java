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

import com.island.ohara.common.annotations.Nullable;
import com.island.ohara.common.data.Row;
import com.island.ohara.common.data.Serializer;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.source.SourceRecord;

/** A wrap to SourceRecord. Currently, only value schema and value are changed. */
public class RowSourceRecord {
  private final Map<String, ?> sourcePartition;
  private final Map<String, ?> sourceOffset;
  private final String topic;

  @Nullable("thanks to kafka")
  private final Integer partition;

  private final Row row;

  @Nullable("thanks to kafka")
  private final Long timestamp;

  private RowSourceRecord(
      Map<String, ?> sourcePartition,
      Map<String, ?> sourceOffset,
      String topic,
      Integer partition,
      Row row,
      Long timestamp) {
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
    return Optional.ofNullable(partition);
  }

  public Row row() {
    return row;
  }

  public Optional<Long> timestamp() {
    return Optional.ofNullable(timestamp);
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
    return builder().row(row).topic(topic).build();
  }

  /**
   * a helper method used to handle the fucking null produced by kafka...
   *
   * @param record kafka's source
   * @return ohara's source
   */
  static RowSourceRecord of(SourceRecord record) {
    Builder builder = new Builder();
    builder.topic(record.topic());
    // kakfa fucking love null!!! We have got to handle the null manually....
    if (record.sourceOffset() != null) builder.sourceOffset(record.sourceOffset());
    if (record.sourcePartition() != null) builder.sourcePartition(record.sourcePartition());
    if (record.kafkaPartition() != null) builder.partition(record.kafkaPartition());
    if (record.timestamp() != null) builder.timestamp(record.timestamp());
    builder.row(Serializer.ROW.from((byte[]) record.value()));
    return builder.build();
  }

  /**
   * a helper method used to handle the fucking null produced by kafka...
   *
   * @return kafka's source
   */
  SourceRecord toSourceRecord() {
    return new SourceRecord(
        sourcePartition(),
        sourceOffset(),
        topic(),
        partition,
        Schema.BYTES_SCHEMA,
        key(),
        Schema.BYTES_SCHEMA,
        Serializer.ROW.to(row()),
        timestamp);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Builder() {
      // do nothing
    }

    private Map<String, ?> sourcePartition = Collections.emptyMap();
    private Map<String, ?> sourceOffset = Collections.emptyMap();
    private Integer partition = null;
    private Row row = null;
    private Long timestamp = null;
    private String topic = null;

    @com.island.ohara.common.annotations.Optional("default is empty")
    public Builder sourcePartition(Map<String, ?> sourcePartition) {
      this.sourcePartition = Objects.requireNonNull(sourcePartition);
      return this;
    }

    @com.island.ohara.common.annotations.Optional("default is empty")
    public Builder sourceOffset(Map<String, ?> sourceOffset) {
      this.sourceOffset = Objects.requireNonNull(sourceOffset);
      return this;
    }

    @com.island.ohara.common.annotations.Optional(
        "default is empty. It means target partition is computed by hash")
    public Builder partition(int partition) {
      this.partition = partition;
      return this;
    }

    public Builder row(Row row) {
      this.row = Objects.requireNonNull(row);
      return this;
    }

    @com.island.ohara.common.annotations.Optional("default is current time")
    public Builder timestamp(long timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    public Builder topic(String topic) {
      this.topic = topic;
      return this;
    }

    public RowSourceRecord build() {
      return new RowSourceRecord(
          Objects.requireNonNull(sourcePartition),
          Objects.requireNonNull(sourceOffset),
          Objects.requireNonNull(topic),
          partition,
          Objects.requireNonNull(row),
          timestamp);
    }
  }
}
