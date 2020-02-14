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

import com.island.ohara.common.rule.OharaTest;
import com.island.ohara.common.setting.SettingDef;
import com.island.ohara.common.setting.WithDefinitions;
import com.island.ohara.common.util.CommonUtils;
import com.island.ohara.kafka.connector.DumbSink;
import org.apache.kafka.common.config.ConfigDef;
import org.junit.Assert;
import org.junit.Test;

public class TestCoreDefinitions extends OharaTest {

  @Test
  public void testClassNameDefinition() {
    DumbSink sink = new DumbSink();
    ConfigDef.ConfigKey key =
        sink.config().configKeys().get(ConnectorDefUtils.CONNECTOR_CLASS_DEFINITION.key());
    Assert.assertEquals(ConnectorDefUtils.CONNECTOR_CLASS_DEFINITION.key(), key.name);
    Assert.assertEquals(
        ConnectorDefUtils.CONNECTOR_CLASS_DEFINITION.orderInGroup(), key.orderInGroup);
    Assert.assertEquals(ConnectorDefUtils.CONNECTOR_CLASS_DEFINITION.group(), key.group);
    Assert.assertEquals(
        ConnectorDefUtils.CONNECTOR_CLASS_DEFINITION.valueType().name(), key.type.name());
  }

  @Test
  public void testTopicNamesDefinition() {
    DumbSink sink = new DumbSink();
    ConfigDef.ConfigKey key =
        sink.config().configKeys().get(ConnectorDefUtils.TOPIC_NAMES_DEFINITION.key());
    Assert.assertEquals(ConnectorDefUtils.TOPIC_NAMES_DEFINITION.key(), key.name);
    Assert.assertEquals(ConnectorDefUtils.TOPIC_NAMES_DEFINITION.orderInGroup(), key.orderInGroup);
    Assert.assertEquals(ConnectorDefUtils.TOPIC_NAMES_DEFINITION.group(), key.group);
    Assert.assertEquals(
        ConnectorDefUtils.TOPIC_NAMES_DEFINITION.valueType(), SettingDef.Type.ARRAY);
  }

  @Test
  public void testNumberOfTasksDefinition() {
    DumbSink sink = new DumbSink();
    ConfigDef.ConfigKey key =
        sink.config().configKeys().get(ConnectorDefUtils.NUMBER_OF_TASKS_DEFINITION.key());
    Assert.assertEquals(ConnectorDefUtils.NUMBER_OF_TASKS_DEFINITION.key(), key.name);
    Assert.assertEquals(
        ConnectorDefUtils.NUMBER_OF_TASKS_DEFINITION.orderInGroup(), key.orderInGroup);
    Assert.assertEquals(ConnectorDefUtils.NUMBER_OF_TASKS_DEFINITION.group(), key.group);
    Assert.assertEquals(
        ConnectorDefUtils.NUMBER_OF_TASKS_DEFINITION.valueType().name(),
        SettingDef.Type.POSITIVE_INT.name());
  }

  @Test
  public void testWorkerClusterNameDefinition() {
    DumbSink sink = new DumbSink();
    ConfigDef.ConfigKey key =
        sink.config().configKeys().get(ConnectorDefUtils.WORKER_CLUSTER_KEY_DEFINITION.key());
    Assert.assertEquals(ConnectorDefUtils.WORKER_CLUSTER_KEY_DEFINITION.key(), key.name);
    Assert.assertEquals(
        ConnectorDefUtils.WORKER_CLUSTER_KEY_DEFINITION.orderInGroup(), key.orderInGroup);
    Assert.assertEquals(ConnectorDefUtils.WORKER_CLUSTER_KEY_DEFINITION.group(), key.group);
  }

  @Test
  public void testColumnsDefinition() {
    DumbSink sink = new DumbSink();
    ConfigDef.ConfigKey key =
        sink.config().configKeys().get(ConnectorDefUtils.COLUMNS_DEFINITION.key());
    Assert.assertEquals(ConnectorDefUtils.COLUMNS_DEFINITION.key(), key.name);
    Assert.assertEquals(ConnectorDefUtils.COLUMNS_DEFINITION.orderInGroup(), key.orderInGroup);
    Assert.assertEquals(ConnectorDefUtils.COLUMNS_DEFINITION.group(), key.group);
    // the TABLE is mapped to STRING
    Assert.assertEquals(SettingDef.Type.STRING.name(), key.type.name());
  }

  @Test
  public void testVersionDefinition() {
    DumbSink sink = new DumbSink();
    ConfigDef.ConfigKey key = sink.config().configKeys().get(WithDefinitions.VERSION_KEY);
    Assert.assertEquals(WithDefinitions.VERSION_KEY, key.name);
    Assert.assertEquals(WithDefinitions.VERSION_ORDER, key.orderInGroup);
  }

  @Test
  public void testRevisionDefinition() {
    DumbSink sink = new DumbSink();
    ConfigDef.ConfigKey key = sink.config().configKeys().get(WithDefinitions.REVISION_KEY);
    Assert.assertEquals(WithDefinitions.REVISION_KEY, key.name);
    Assert.assertEquals(WithDefinitions.REVISION_ORDER, key.orderInGroup);
  }

  @Test
  public void testAuthorDefinition() {
    DumbSink sink = new DumbSink();
    ConfigDef.ConfigKey key = sink.config().configKeys().get(WithDefinitions.AUTHOR_KEY);
    Assert.assertEquals(WithDefinitions.AUTHOR_KEY, key.name);
    Assert.assertEquals(WithDefinitions.AUTHOR_ORDER, key.orderInGroup);
  }

  @Test
  public void testIdSetting() {
    DumbSink sink = new DumbSink();
    ConfigDef.ConfigKey key =
        sink.config().configKeys().get(ConnectorDefUtils.CONNECTOR_NAME_DEFINITION.key());
    Assert.assertEquals(ConnectorDefUtils.CONNECTOR_NAME_DEFINITION.key(), key.name);
    Assert.assertEquals(
        ConnectorDefUtils.CONNECTOR_NAME_DEFINITION.orderInGroup(), key.orderInGroup);
    Assert.assertEquals(ConnectorDefUtils.CONNECTOR_NAME_DEFINITION.group(), key.group);
    Assert.assertEquals(
        ConnectorDefUtils.CONNECTOR_NAME_DEFINITION.valueType().name(), key.type.name());
  }

  @Test
  public void testConnectorType() {
    DumbSink sink = new DumbSink();
    ConfigDef.ConfigKey key = sink.config().configKeys().get(ConnectorDefUtils.KIND_KEY);
    Assert.assertEquals(ConnectorDefUtils.KIND_KEY, key.name);
    Assert.assertEquals(ConnectorDefUtils.SOURCE_KIND_DEFINITION.orderInGroup(), key.orderInGroup);
    Assert.assertEquals(ConnectorDefUtils.SOURCE_KIND_DEFINITION.group(), key.group);
    Assert.assertEquals(
        ConnectorDefUtils.SOURCE_KIND_DEFINITION.valueType().name(), key.type.name());
  }

  @Test(expected = NullPointerException.class)
  public void nullVersionInSource() {
    new SourceWithNullableSetting(null, CommonUtils.randomString(), CommonUtils.randomString())
        .config();
  }

  @Test(expected = NullPointerException.class)
  public void nullRevisionInSource() {
    new SourceWithNullableSetting(CommonUtils.randomString(), null, CommonUtils.randomString())
        .config();
  }

  @Test(expected = NullPointerException.class)
  public void nullAuthorInSource() {
    new SourceWithNullableSetting(CommonUtils.randomString(), CommonUtils.randomString(), null)
        .config();
  }

  @Test(expected = NullPointerException.class)
  public void nullVersionInSink() {
    new SourceWithNullableSetting(null, CommonUtils.randomString(), CommonUtils.randomString())
        .config();
  }

  @Test(expected = NullPointerException.class)
  public void nullRevisionInSink() {
    new SinkWithNullableSetting(CommonUtils.randomString(), null, CommonUtils.randomString())
        .config();
  }

  @Test(expected = NullPointerException.class)
  public void nullAuthorInSink() {
    new SinkWithNullableSetting(CommonUtils.randomString(), CommonUtils.randomString(), null)
        .config();
  }
}
