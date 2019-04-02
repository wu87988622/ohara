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

package com.island.ohara.kafka.connector.json;

import com.island.ohara.common.rule.SmallTest;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Test;

public class TestSettingDefinitions extends SmallTest {

  @Test(expected = NoSuchElementException.class)
  public void noVersion() {
    SettingDefinitions.version(SettingDefinitions.DEFINITIONS_DEFAULT);
  }

  @Test(expected = NoSuchElementException.class)
  public void noRevision() {
    SettingDefinitions.revision(SettingDefinitions.DEFINITIONS_DEFAULT);
  }

  @Test(expected = NoSuchElementException.class)
  public void noAuthor() {
    SettingDefinitions.author(SettingDefinitions.DEFINITIONS_DEFAULT);
  }

  @Test
  public void testConnectorClass() {
    Assert.assertEquals(
        1,
        SettingDefinitions.DEFINITIONS_DEFAULT.stream()
            .filter(d -> d.equals(SettingDefinition.CONNECTOR_CLASS_DEFINITION))
            .count());
  }

  @Test
  public void testTopics() {
    Assert.assertEquals(
        1,
        SettingDefinitions.DEFINITIONS_DEFAULT.stream()
            .filter(d -> d.equals(SettingDefinition.TOPIC_NAMES_DEFINITION))
            .count());
  }

  @Test
  public void testColumns() {
    Assert.assertEquals(
        1,
        SettingDefinitions.DEFINITIONS_DEFAULT.stream()
            .filter(d -> d.equals(SettingDefinition.COLUMNS_DEFINITION))
            .count());
  }

  @Test
  public void testKeyConverter() {
    Assert.assertEquals(
        1,
        SettingDefinitions.DEFINITIONS_DEFAULT.stream()
            .filter(d -> d.equals(SettingDefinition.KEY_CONVERTER_DEFINITION))
            .count());
  }

  @Test
  public void testValueConverter() {
    Assert.assertEquals(
        1,
        SettingDefinitions.DEFINITIONS_DEFAULT.stream()
            .filter(d -> d.equals(SettingDefinition.VALUE_CONVERTER_DEFINITION))
            .count());
  }

  @Test
  public void testWorkerClusterName() {
    Assert.assertEquals(
        1,
        SettingDefinitions.DEFINITIONS_DEFAULT.stream()
            .filter(d -> d.equals(SettingDefinition.WORKER_CLUSTER_NAME_DEFINITION))
            .count());
  }

  @Test
  public void testNumberOfTasks() {
    Assert.assertEquals(
        1,
        SettingDefinitions.DEFINITIONS_DEFAULT.stream()
            .filter(d -> d.equals(SettingDefinition.NUMBER_OF_TASKS_DEFINITION))
            .count());
  }

  @Test
  public void mustHaveTable() {
    Assert.assertEquals(
        1,
        SettingDefinitions.DEFINITIONS_DEFAULT.stream()
            .filter(
                definition -> definition.valueType().equals(SettingDefinition.Type.TABLE.name()))
            .count());
  }

  @Test
  public void testPropKeys() {
    SettingDefinitions.DEFINITIONS_DEFAULT.forEach(
        definition -> {
          if (definition.valueType().equals(SettingDefinition.Type.TABLE.name())) {
            Assert.assertTrue(definition.tableKeys().contains(SettingDefinition.ORDER_KEY));
            Assert.assertTrue(
                definition.tableKeys().contains(SettingDefinition.COLUMN_DATA_TYPE_KEY));
            Assert.assertTrue(definition.tableKeys().contains(SettingDefinition.COLUMN_NAME_KEY));
            Assert.assertTrue(
                definition.tableKeys().contains(SettingDefinition.COLUMN_NEW_NAME_KEY));
          } else Assert.assertTrue(definition.tableKeys().isEmpty());
        });
  }

  @Test
  public void mustHaveConnectorName() {
    Assert.assertEquals(
        1,
        SettingDefinitions.DEFINITIONS_DEFAULT.stream()
            .filter(
                definition ->
                    definition.key().equals(SettingDefinition.CONNECTOR_NAME_DEFINITION.key()))
            .count());

    SettingDefinition nameDefinition =
        SettingDefinitions.DEFINITIONS_DEFAULT.stream()
            .filter(
                definition ->
                    definition.key().equals(SettingDefinition.CONNECTOR_NAME_DEFINITION.key()))
            .findFirst()
            .get();
    Assert.assertNull(nameDefinition.defaultValue());
    Assert.assertFalse(nameDefinition.required());
  }

  @Test
  public void checkDuplicate() {
    Assert.assertEquals(
        SettingDefinitions.DEFINITIONS_DEFAULT.size(),
        SettingDefinitions.DEFINITIONS_DEFAULT.stream()
            .map(SettingDefinition::key)
            .collect(Collectors.toSet())
            .size());
  }
}
