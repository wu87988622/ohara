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
import com.island.ohara.common.util.CommonUtils;
import java.util.Objects;
import org.apache.kafka.connect.sink.SinkRecord;

/**
 * The methods it have are almost same with SinkRecord. It return Table rather than any object.
 * Also, it doesn't have method to return value schema because the value schema is useless to user.
 */
public class RowSinkRecord {
  /**
   * The timestamp type of the records. NOTED: those names MUST be same with
   * org.apache.kafka.common.record.TimestampType
   */
  enum TimestampType {
    NO_TIMESTAMP_TYPE,
    CREATE_TIME,
    LOG_APPEND_TIME;

    private static TimestampType to(org.apache.kafka.common.record.TimestampType type) {
      if (type == org.apache.kafka.common.record.TimestampType.NO_TIMESTAMP_TYPE)
        return NO_TIMESTAMP_TYPE;
      if (type == org.apache.kafka.common.record.TimestampType.CREATE_TIME) return CREATE_TIME;
      if (type == org.apache.kafka.common.record.TimestampType.LOG_APPEND_TIME)
        return LOG_APPEND_TIME;
      throw new IllegalArgumentException("unknown " + type);
    }

    private static org.apache.kafka.common.record.TimestampType to(TimestampType type) {
      switch (type) {
        case NO_TIMESTAMP_TYPE:
          return org.apache.kafka.common.record.TimestampType.NO_TIMESTAMP_TYPE;
        case CREATE_TIME:
          return org.apache.kafka.common.record.TimestampType.CREATE_TIME;
        case LOG_APPEND_TIME:
          return org.apache.kafka.common.record.TimestampType.LOG_APPEND_TIME;
        default:
          throw new IllegalArgumentException("unknown " + type);
      }
    }
  }

  private final String topic;
  private final Row row;
  private final int partition;
  private final long offset;
  private final long timestamp;
  private final TimestampType tsType;

  private RowSinkRecord(
      String topic, Row row, int partition, long offset, long timestamp, TimestampType tsType) {
    this.topic = topic;
    this.row = row;
    this.partition = partition;
    this.offset = offset;
    this.timestamp = timestamp;
    this.tsType = tsType;
  }

  public String topic() {
    return topic;
  }

  public Row row() {
    return row;
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

  public TimestampType timestampType() {
    return tsType;
  }

  /**
   * @param record kafka's sink record
   * @return ohara's sink record
   */
  static RowSinkRecord of(SinkRecord record) {
    return builder()
        .topic(record.topic())
        .row(Serializer.ROW.from((byte[]) record.key()))
        .partition(record.kafkaPartition())
        .offset(record.kafkaOffset())
        .timestamp(record.timestamp())
        .timestampType(TimestampType.to(record.timestampType()))
        .build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Builder() {
      // do nothing
    }

    private String topic;
    private Row row;
    private Integer partition;
    private Long offset;
    private Long timestamp;
    private TimestampType tsType;

    public Builder topic(String topic) {
      this.topic = Objects.requireNonNull(topic);
      return this;
    }

    public Builder row(Row row) {
      this.row = Objects.requireNonNull(row);
      return this;
    }

    public Builder partition(int partition) {
      this.partition = CommonUtils.requirePositiveInt(partition);
      return this;
    }

    public Builder offset(long offset) {
      this.offset = CommonUtils.requirePositiveLong(offset);
      return this;
    }

    public Builder timestamp(long timestamp) {
      this.timestamp = CommonUtils.requirePositiveLong(timestamp);
      return this;
    }

    public Builder timestampType(TimestampType tsType) {
      this.tsType = Objects.requireNonNull(tsType);
      return this;
    }

    public RowSinkRecord build() {
      return new RowSinkRecord(
          Objects.requireNonNull(topic),
          Objects.requireNonNull(row),
          Objects.requireNonNull(partition),
          Objects.requireNonNull(offset),
          Objects.requireNonNull(timestamp),
          Objects.requireNonNull(tsType));
    }
  }
}
