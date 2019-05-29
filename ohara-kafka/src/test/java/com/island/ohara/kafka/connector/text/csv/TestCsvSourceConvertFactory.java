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

package com.island.ohara.kafka.connector.text.csv;

import com.island.ohara.common.data.Column;
import com.island.ohara.common.data.DataType;
import com.island.ohara.common.rule.SmallTest;
import com.island.ohara.kafka.connector.RowSourceContext;
import com.island.ohara.kafka.connector.TaskSetting;
import com.island.ohara.kafka.connector.json.PropGroups;
import com.island.ohara.kafka.connector.json.SettingDefinition;
import java.util.*;
import org.junit.Assert;
import org.junit.Test;

public class TestCsvSourceConvertFactory extends SmallTest {

  private final String path = getClass().getName();

  private TaskSetting createConfig() {
    Column column =
        Column.builder().name("cf1").newName("cf1").dataType(DataType.STRING).order(1).build();
    String columnJsonString =
        PropGroups.ofColumns(Collections.singletonList(column)).toJsonString();

    Map<String, String> props = new HashMap<>();
    props.put(SettingDefinition.TOPIC_NAMES_DEFINITION.key(), "T1, T2");
    props.put(SettingDefinition.COLUMNS_DEFINITION.key(), columnJsonString);
    return TaskSetting.of(props);
  }

  private RowSourceContext createContext() {
    return new RowSourceContext() {
      @Override
      public <T> Map<String, Object> offset(Map<String, T> partition) {
        return Collections.emptyMap();
      }

      @Override
      public <T> Map<Map<String, T>, Map<String, Object>> offset(List<Map<String, T>> partitions) {
        return Collections.emptyMap();
      }
    };
  }

  @Test
  public void testGetConverter() {
    CsvSourceConverterFactory factory = new CsvSourceConverterFactory(createConfig());
    Assert.assertTrue(factory.newConverter(createContext(), path) instanceof CsvSourceConverter);
  }

  @Test(expected = NoSuchElementException.class)
  public void testGetConverter_WithEmptySettings() {
    new CsvSourceConverterFactory(TaskSetting.of(Collections.emptyMap()));
  }

  @Test(expected = NullPointerException.class)
  public void testGetConverter_WithNonSettings() {
    new CsvSourceConverterFactory(null);
  }
}
