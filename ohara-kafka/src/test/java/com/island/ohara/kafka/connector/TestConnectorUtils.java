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

import com.island.ohara.common.data.Cell;
import com.island.ohara.common.data.Column;
import com.island.ohara.common.data.DataType;
import com.island.ohara.common.data.Row;
import com.island.ohara.common.rule.OharaTest;
import com.island.ohara.common.util.ByteUtils;
import com.island.ohara.common.util.CommonUtils;
import java.util.Collections;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class TestConnectorUtils extends OharaTest {
  @Test
  public void testSizeOfBytes() {
    Assert.assertNotEquals(ConnectorUtils.sizeOf(new byte[10]), 0);
  }

  @Test
  public void testSizeOfRow() {
    Assert.assertNotEquals(ConnectorUtils.sizeOf(Row.of(Cell.of("a", "v"))), 0);
  }

  @Test
  public void testSizeOfShort() {
    Assert.assertEquals(ConnectorUtils.sizeOf((short) 123), ByteUtils.SIZE_OF_SHORT);
  }

  @Test
  public void testSizeOfInt() {
    Assert.assertEquals(ConnectorUtils.sizeOf(123), ByteUtils.SIZE_OF_INT);
  }

  @Test
  public void testSizeOfLong() {
    Assert.assertEquals(ConnectorUtils.sizeOf(123L), ByteUtils.SIZE_OF_LONG);
  }

  @Test
  public void testSizeOfFloat() {
    Assert.assertEquals(ConnectorUtils.sizeOf((float) 123), ByteUtils.SIZE_OF_FLOAT);
  }

  @Test
  public void testSizeOfDouble() {
    Assert.assertEquals(ConnectorUtils.sizeOf((double) 123), ByteUtils.SIZE_OF_DOUBLE);
  }

  @Test
  public void testSizeOfBoolean() {
    Assert.assertEquals(ConnectorUtils.sizeOf(false), ByteUtils.SIZE_OF_BOOLEAN);
  }

  @Test
  public void testSizeOfString() {
    Assert.assertEquals(ConnectorUtils.sizeOf("abc"), 3);
  }

  @Test
  public void testSizeOfEmptyRecord() {
    ConnectRecord<?> record = Mockito.mock(ConnectRecord.class);
    Mockito.when(record.key()).thenReturn(null);
    Mockito.when(record.value()).thenReturn(null);
    Assert.assertEquals(ConnectorUtils.sizeOf(record), 0);
  }

  @Test
  public void testMatch() {
    Column column =
        Column.builder()
            .name(CommonUtils.randomString())
            .newName(CommonUtils.randomString())
            .dataType(DataType.STRING)
            .build();

    // test illegal type
    assertException(
        IllegalArgumentException.class,
        () ->
            ConnectorUtils.match(
                Row.of(Cell.of(column.name(), 123)), Collections.singletonList(column), true));

    // test illegal name
    assertException(
        IllegalArgumentException.class,
        () ->
            ConnectorUtils.match(
                Row.of(Cell.of(CommonUtils.randomString(), 123)),
                Collections.singletonList(column),
                true));

    // pass
    ConnectorUtils.match(
        Row.of(Cell.of(column.name(), CommonUtils.randomString())),
        Collections.singletonList(column),
        true);

    // test illegal type
    assertException(
        IllegalArgumentException.class,
        () ->
            ConnectorUtils.match(
                Row.of(Cell.of(column.newName(), 123)), Collections.singletonList(column), false));

    // test illegal name
    assertException(
        IllegalArgumentException.class,
        () ->
            ConnectorUtils.match(
                Row.of(Cell.of(CommonUtils.randomString(), 123)),
                Collections.singletonList(column),
                false));

    // pass
    ConnectorUtils.match(
        Row.of(Cell.of(column.newName(), CommonUtils.randomString())),
        Collections.singletonList(column),
        false);
  }
}
