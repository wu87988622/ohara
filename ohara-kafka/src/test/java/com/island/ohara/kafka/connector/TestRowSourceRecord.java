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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.island.ohara.common.data.Cell;
import com.island.ohara.common.data.Row;
import com.island.ohara.common.rule.SmallTest;
import com.island.ohara.common.util.CommonUtils;
import java.util.Collections;
import java.util.Map;
import org.junit.Test;

public class TestRowSourceRecord extends SmallTest {

  @Test(expected = NullPointerException.class)
  public void requireTopic() {
    RowSourceRecord.builder().row(Row.of(Cell.of(methodName(), 123))).build();
  }

  @Test(expected = NullPointerException.class)
  public void requireRow() {
    RowSourceRecord.builder().topic("Adasd").build();
  }

  @Test
  public void testBuilderWithDefaultValue() {
    Row row = Row.of(Cell.of(methodName(), 123));
    String topic = methodName();

    RowSourceRecord r = RowSourceRecord.builder().topic(topic).row(row).build();
    assertEquals(topic, r.topic());
    assertEquals(row, r.row());
    assertFalse(r.partition().isPresent());
    assertFalse(r.timestamp().isPresent());
    assertTrue(r.sourceOffset().isEmpty());
    assertTrue(r.sourcePartition().isEmpty());
  }

  @Test
  public void testBuilder() {
    Row row = Row.of(Cell.of(methodName(), 123));
    String topic = methodName();
    long ts = CommonUtils.current();
    int partition = 123;
    Map<String, String> sourceOffset = Collections.singletonMap("abc", "ddd");
    Map<String, String> sourcePartition = Collections.singletonMap("abc", "ddd");

    RowSourceRecord r =
        RowSourceRecord.builder()
            .topic(topic)
            .row(row)
            .timestamp(ts)
            .partition(partition)
            .sourceOffset(sourceOffset)
            .sourcePartition(sourcePartition)
            .build();
    assertEquals(topic, r.topic());
    assertEquals(row, r.row());
    assertEquals(ts, (long) r.timestamp().get());
    assertEquals(partition, (int) r.partition().get());
    assertEquals(sourceOffset, r.sourceOffset());
    assertEquals(sourcePartition, r.sourcePartition());
  }
}
