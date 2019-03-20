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
import com.island.ohara.kafka.connector.DumbSink;
import org.apache.kafka.common.config.ConfigDef;
import org.junit.Assert;
import org.junit.Test;

public class TestCoreDefinitions extends SmallTest {

  @Test
  public void testNameDefinition() {
    DumbSink sink = new DumbSink();
    ConfigDef.ConfigKey key = sink.config().configKeys().get(ConnectorFormatter.NAME_KEY);
    Assert.assertEquals(ConnectorFormatter.NAME_KEY, key.name);
    Assert.assertEquals(ConnectorFormatter.NAME_KEY_ORDER, key.orderInGroup);
    Assert.assertEquals(SettingDefinition.CORE_GROUP, key.group);
    Assert.assertEquals(SettingDefinition.Type.STRING.name(), key.type.name());
  }

  @Test
  public void testClassNameDefinition() {
    DumbSink sink = new DumbSink();
    ConfigDef.ConfigKey key = sink.config().configKeys().get(ConnectorFormatter.CLASS_NAME_KEY);
    Assert.assertEquals(ConnectorFormatter.CLASS_NAME_KEY, key.name);
    Assert.assertEquals(ConnectorFormatter.CLASS_NAME_KEY_ORDER, key.orderInGroup);
    Assert.assertEquals(SettingDefinition.CORE_GROUP, key.group);
    Assert.assertEquals(SettingDefinition.Type.CLASS.name(), key.type.name());
  }

  @Test
  public void testTopicNamesDefinition() {
    DumbSink sink = new DumbSink();
    ConfigDef.ConfigKey key = sink.config().configKeys().get(ConnectorFormatter.TOPIC_NAMES_KEY);
    Assert.assertEquals(ConnectorFormatter.TOPIC_NAMES_KEY, key.name);
    Assert.assertEquals(ConnectorFormatter.TOPIC_NAMES_KEY_ORDER, key.orderInGroup);
    Assert.assertEquals(SettingDefinition.CORE_GROUP, key.group);
    Assert.assertEquals(SettingDefinition.Type.LIST.name(), key.type.name());
  }

  @Test
  public void testNumberOfTasksDefinition() {
    DumbSink sink = new DumbSink();
    ConfigDef.ConfigKey key =
        sink.config().configKeys().get(ConnectorFormatter.NUMBER_OF_TASKS_KEY);
    Assert.assertEquals(ConnectorFormatter.NUMBER_OF_TASKS_KEY, key.name);
    Assert.assertEquals(ConnectorFormatter.NUMBER_OF_TASKS_KEY_ORDER, key.orderInGroup);
    Assert.assertEquals(SettingDefinition.CORE_GROUP, key.group);
    Assert.assertEquals(SettingDefinition.Type.INT.name(), key.type.name());
  }

  @Test
  public void testWorkerClusterNameDefinition() {
    DumbSink sink = new DumbSink();
    ConfigDef.ConfigKey key =
        sink.config().configKeys().get(ConnectorFormatter.WORKER_CLUSTER_NAME_KEY);
    Assert.assertEquals(ConnectorFormatter.WORKER_CLUSTER_NAME_KEY, key.name);
    Assert.assertEquals(ConnectorFormatter.WORKER_CLUSTER_NAME_KEY_ORDER, key.orderInGroup);
    Assert.assertEquals(SettingDefinition.CORE_GROUP, key.group);
    Assert.assertEquals(SettingDefinition.Type.STRING.name(), key.type.name());
  }

  @Test
  public void testColumnDefinition() {
    DumbSink sink = new DumbSink();
    ConfigDef.ConfigKey key = sink.config().configKeys().get(ConnectorFormatter.COLUMNS_KEY);
    Assert.assertEquals(ConnectorFormatter.COLUMNS_KEY, key.name);
    Assert.assertEquals(ConnectorFormatter.COLUMNS_KEY_ORDER, key.orderInGroup);
    Assert.assertEquals(SettingDefinition.CORE_GROUP, key.group);
    // the TABLE is mapped to STRING
    Assert.assertEquals(SettingDefinition.Type.STRING.name(), key.type.name());
  }

  @Test
  public void testVersionDefinition() {
    DumbSink sink = new DumbSink();
    ConfigDef.ConfigKey key = sink.config().configKeys().get(ConnectorFormatter.VERSION_KEY);
    Assert.assertEquals(ConnectorFormatter.VERSION_KEY, key.name);
    Assert.assertEquals(ConnectorFormatter.VERSION_KEY_ORDER, key.orderInGroup);
    Assert.assertEquals(SettingDefinition.CORE_GROUP, key.group);
    Assert.assertEquals(SettingDefinition.Type.STRING.name(), key.type.name());
  }

  @Test
  public void testRevisionDefinition() {
    DumbSink sink = new DumbSink();
    ConfigDef.ConfigKey key = sink.config().configKeys().get(ConnectorFormatter.REVISION_KEY);
    Assert.assertEquals(ConnectorFormatter.REVISION_KEY, key.name);
    Assert.assertEquals(ConnectorFormatter.REVISION_KEY_ORDER, key.orderInGroup);
    Assert.assertEquals(SettingDefinition.CORE_GROUP, key.group);
    Assert.assertEquals(SettingDefinition.Type.STRING.name(), key.type.name());
  }

  @Test
  public void testAuthorDefinition() {
    DumbSink sink = new DumbSink();
    ConfigDef.ConfigKey key = sink.config().configKeys().get(ConnectorFormatter.AUTHOR_KEY);
    Assert.assertEquals(ConnectorFormatter.AUTHOR_KEY, key.name);
    Assert.assertEquals(ConnectorFormatter.AUTHOR_KEY_ORDER, key.orderInGroup);
    Assert.assertEquals(SettingDefinition.CORE_GROUP, key.group);
    Assert.assertEquals(SettingDefinition.Type.STRING.name(), key.type.name());
  }
}
