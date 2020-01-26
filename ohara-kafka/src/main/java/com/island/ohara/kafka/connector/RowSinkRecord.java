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
import com.island.ohara.common.util.CommonUtils;
import com.island.ohara.kafka.TimestampType;
import java.util.Objects;

/**
 * The methods it have are almost same with SinkRecord. It return Table rather than any object.
 * Also, it doesn't have method to return value schema because the value schema is useless to user.
 */
public class RowSinkRecord {

  private final String topicName;
  private final Row row;
  private final int partition;
  private final long offset;
  private final long timestamp;
  private final TimestampType timestampType;

  private RowSinkRecord(
      String topicName,
      Row row,
      int partition,
      long offset,
      long timestamp,
      TimestampType timestampType) {
    this.topicName = CommonUtils.requireNonEmpty(topicName);
    this.row = Objects.requireNonNull(row);
    this.partition = partition;
    this.offset = offset;
    this.timestamp = timestamp;
    this.timestampType = Objects.requireNonNull(timestampType);
  }

  public String topicName() {
    return topicName;
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
    return timestampType;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder implements com.island.ohara.common.pattern.Builder<RowSinkRecord> {
    private Builder() {
      // do nothing
    }

    private String topicName;
    private Row row;
    private Integer partition;
    private Long offset;
    private Long timestamp;
    private TimestampType timestampType;

    public Builder topicName(String topicName) {
      this.topicName = CommonUtils.requireNonEmpty(topicName);
      return this;
    }

    public Builder row(Row row) {
      this.row = Objects.requireNonNull(row);
      return this;
    }

    public Builder partition(int partition) {
      this.partition = CommonUtils.requireNonNegativeInt(partition);
      return this;
    }

    public Builder offset(long offset) {
      this.offset = CommonUtils.requireNonNegativeLong(offset);
      return this;
    }

    public Builder timestamp(long timestamp) {
      this.timestamp = CommonUtils.requireNonNegativeLong(timestamp);
      return this;
    }

    public Builder timestampType(TimestampType timestampType) {
      this.timestampType = Objects.requireNonNull(timestampType);
      return this;
    }

    @Override
    public RowSinkRecord build() {
      return new RowSinkRecord(
          CommonUtils.requireNonEmpty(topicName),
          Objects.requireNonNull(row),
          Objects.requireNonNull(partition),
          Objects.requireNonNull(offset),
          Objects.requireNonNull(timestamp),
          Objects.requireNonNull(timestampType));
    }
  }
}
