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

import com.island.ohara.common.data.Column;
import com.island.ohara.common.data.DataType;
import com.island.ohara.common.rule.SmallTest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.kafka.common.config.ConfigDef;
import org.junit.Assert;
import org.junit.Test;

public class TestConnectorUtils extends SmallTest {

  @Test
  public void testDefaultDefinitions() {
    ConnectorUtils.DEFINITIONS_DEFAULT.forEach(
        definition -> {
          if (!definition.name().equals(ConnectorUtils.TOPIC_NAMES_KEY)
              && definition.name().equals(ConnectorUtils.NUMBER_OF_TASKS_KEY)
              && definition.name().equals(ConnectorUtils.CONNECTOR_CLASS_KEY)
              && definition.name().equals(ConnectorUtils.NAME_KEY)
              && definition.name().equals(ConnectorUtils.COLUMNS_KEY))
            throw new RuntimeException("Please check the default definition");
        });
  }

  @Test
  public void testToConfigDefWithEmpty() {
    ConfigDef def = ConnectorUtils.toConfigDef(Collections.emptyList(), false);
    Assert.assertTrue(def.configKeys().isEmpty());
  }

  @Test
  public void testToConfigDefWithDefault() {
    ConfigDef def = ConnectorUtils.toConfigDef(Collections.emptyList(), true);
    Assert.assertTrue(def.configKeys().containsKey(ConnectorUtils.NAME_KEY));
    Assert.assertTrue(def.configKeys().containsKey(ConnectorUtils.TOPIC_NAMES_KEY));
    Assert.assertTrue(def.configKeys().containsKey(ConnectorUtils.CONNECTOR_CLASS_KEY));
    Assert.assertTrue(def.configKeys().containsKey(ConnectorUtils.NUMBER_OF_TASKS_KEY));
  }

  @Test
  public void testEmptyColumn() {
    Assert.assertTrue(ConnectorUtils.toColumns("").isEmpty());
  }

  @Test
  public void testNullColumn() {
    Assert.assertTrue(ConnectorUtils.toColumns(null).isEmpty());
  }

  @Test
  public void testColumnString() {
    List<Column> columns =
        Arrays.asList(
            Column.newBuilder().name("a").dataType(DataType.STRING).order(0).build(),
            Column.newBuilder().name("c").newName("d").dataType(DataType.INT).order(1).build(),
            Column.newBuilder().name("e").newName("f").dataType(DataType.DOUBLE).order(2).build());

    List<Column> another = ConnectorUtils.toColumns(ConnectorUtils.fromColumns(columns));

    Assert.assertEquals(columns.size(), another.size());
    columns.forEach(
        column ->
            Assert.assertEquals(
                column,
                another.stream().filter(c -> c.name().equals(column.name())).findFirst().get()));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testIllegalNameInColumn() {
    ConnectorUtils.fromColumns(
        Collections.singletonList(
            Column.newBuilder().name("a,").dataType(DataType.STRING).order(0).build()));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testIllegalNewNameInColumn() {
    ConnectorUtils.fromColumns(
        Collections.singletonList(
            Column.newBuilder()
                .name("a")
                .newName("aaa,")
                .dataType(DataType.STRING)
                .order(0)
                .build()));
  }
}
