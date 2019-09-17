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

import static org.junit.Assert.assertEquals;

import com.island.ohara.common.data.Cell;
import com.island.ohara.common.data.Row;
import com.island.ohara.common.rule.SmallTest;
import com.island.ohara.common.util.CommonUtils;
import com.island.ohara.kafka.TimestampType;
import org.junit.Test;

public class TestRowSinkRecord extends SmallTest {

  @Test(expected = NullPointerException.class)
  public void nullTopic() {
    RowSinkRecord.builder().topicName(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyTopic() {
    RowSinkRecord.builder().topicName("");
  }

  @Test(expected = NullPointerException.class)
  public void nullRow() {
    RowSinkRecord.builder().row(null);
  }

  @Test(expected = NullPointerException.class)
  public void requireTopic() {
    RowSinkRecord.builder()
        .row(Row.of(Cell.of(CommonUtils.randomString(10), 123)))
        .timestamp(CommonUtils.current())
        .partition(123)
        .timestampType(TimestampType.NO_TIMESTAMP_TYPE)
        .offset(123)
        .build();
  }

  @Test(expected = NullPointerException.class)
  public void requireRow() {
    RowSinkRecord.builder()
        .topicName("asdasd")
        .timestamp(CommonUtils.current())
        .partition(123)
        .timestampType(TimestampType.NO_TIMESTAMP_TYPE)
        .offset(123)
        .build();
  }

  @Test(expected = NullPointerException.class)
  public void requireTimestamp() {
    RowSinkRecord.builder()
        .topicName("asdasd")
        .row(Row.of(Cell.of(CommonUtils.randomString(10), 123)))
        .partition(123)
        .timestampType(TimestampType.NO_TIMESTAMP_TYPE)
        .offset(123)
        .build();
  }

  @Test(expected = NullPointerException.class)
  public void requirePartition() {
    RowSinkRecord.builder()
        .topicName("asdasd")
        .row(Row.of(Cell.of(CommonUtils.randomString(10), 123)))
        .timestamp(CommonUtils.current())
        .timestampType(TimestampType.NO_TIMESTAMP_TYPE)
        .offset(123)
        .build();
  }

  @Test(expected = NullPointerException.class)
  public void requireTimestampType() {
    RowSinkRecord.builder()
        .topicName("asdasd")
        .row(Row.of(Cell.of(CommonUtils.randomString(10), 123)))
        .timestamp(CommonUtils.current())
        .partition(123)
        .offset(123)
        .build();
  }

  @Test(expected = NullPointerException.class)
  public void requireOffset() {
    RowSinkRecord.builder()
        .topicName("asdasd")
        .row(Row.of(Cell.of(CommonUtils.randomString(10), 123)))
        .timestamp(CommonUtils.current())
        .partition(123)
        .timestampType(TimestampType.NO_TIMESTAMP_TYPE)
        .build();
  }

  @Test
  public void testBuilder() {
    Row row = Row.of(Cell.of(CommonUtils.randomString(10), 123));
    String topic = CommonUtils.randomString(10);
    long ts = CommonUtils.current();
    int partition = 123;
    long offset = 12345;
    TimestampType tsType = TimestampType.NO_TIMESTAMP_TYPE;

    RowSinkRecord r =
        RowSinkRecord.builder()
            .topicName(topic)
            .row(row)
            .timestamp(ts)
            .partition(partition)
            .timestampType(tsType)
            .offset(offset)
            .build();
    assertEquals(topic, r.topicName());
    assertEquals(row, r.row());
    assertEquals(ts, r.timestamp());
    assertEquals(partition, r.partition());
    assertEquals(tsType, r.timestampType());
    assertEquals(offset, r.offset());
  }
}
