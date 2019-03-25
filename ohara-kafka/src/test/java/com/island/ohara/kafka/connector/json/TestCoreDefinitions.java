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
import com.island.ohara.common.util.CommonUtils;
import com.island.ohara.kafka.connector.ConnectorVersion;
import com.island.ohara.kafka.connector.DumbSink;
import com.island.ohara.kafka.connector.RowSinkConnector;
import com.island.ohara.kafka.connector.RowSinkTask;
import com.island.ohara.kafka.connector.RowSourceConnector;
import com.island.ohara.kafka.connector.RowSourceTask;
import com.island.ohara.kafka.connector.TaskConfig;
import java.util.List;
import org.apache.kafka.common.config.ConfigDef;
import org.junit.Assert;
import org.junit.Test;

public class TestCoreDefinitions extends SmallTest {

  @Test
  public void testClassNameDefinition() {
    DumbSink sink = new DumbSink();
    ConfigDef.ConfigKey key =
        sink.config().configKeys().get(SettingDefinition.CONNECTOR_CLASS_DEFINITION.key());
    Assert.assertEquals(SettingDefinition.CONNECTOR_CLASS_DEFINITION.key(), key.name);
    Assert.assertEquals(
        SettingDefinition.CONNECTOR_CLASS_DEFINITION.orderInGroup(), key.orderInGroup);
    Assert.assertEquals(SettingDefinition.CONNECTOR_CLASS_DEFINITION.group(), key.group);
    Assert.assertEquals(SettingDefinition.CONNECTOR_CLASS_DEFINITION.valueType(), key.type.name());
  }

  @Test
  public void testTopicNamesDefinition() {
    DumbSink sink = new DumbSink();
    ConfigDef.ConfigKey key =
        sink.config().configKeys().get(SettingDefinition.TOPIC_NAMES_DEFINITION.key());
    Assert.assertEquals(SettingDefinition.TOPIC_NAMES_DEFINITION.key(), key.name);
    Assert.assertEquals(SettingDefinition.TOPIC_NAMES_DEFINITION.orderInGroup(), key.orderInGroup);
    Assert.assertEquals(SettingDefinition.TOPIC_NAMES_DEFINITION.group(), key.group);
    Assert.assertEquals(SettingDefinition.TOPIC_NAMES_DEFINITION.valueType(), key.type.name());
  }

  @Test
  public void testNumberOfTasksDefinition() {
    DumbSink sink = new DumbSink();
    ConfigDef.ConfigKey key =
        sink.config().configKeys().get(SettingDefinition.NUMBER_OF_TASKS_DEFINITION.key());
    Assert.assertEquals(SettingDefinition.NUMBER_OF_TASKS_DEFINITION.key(), key.name);
    Assert.assertEquals(
        SettingDefinition.NUMBER_OF_TASKS_DEFINITION.orderInGroup(), key.orderInGroup);
    Assert.assertEquals(SettingDefinition.NUMBER_OF_TASKS_DEFINITION.group(), key.group);
    Assert.assertEquals(SettingDefinition.NUMBER_OF_TASKS_DEFINITION.valueType(), key.type.name());
  }

  @Test
  public void testWorkerClusterNameDefinition() {
    DumbSink sink = new DumbSink();
    ConfigDef.ConfigKey key =
        sink.config().configKeys().get(SettingDefinition.WORKER_CLUSTER_NAME_DEFINITION.key());
    Assert.assertEquals(SettingDefinition.WORKER_CLUSTER_NAME_DEFINITION.key(), key.name);
    Assert.assertEquals(
        SettingDefinition.WORKER_CLUSTER_NAME_DEFINITION.orderInGroup(), key.orderInGroup);
    Assert.assertEquals(SettingDefinition.WORKER_CLUSTER_NAME_DEFINITION.group(), key.group);
    Assert.assertEquals(
        SettingDefinition.WORKER_CLUSTER_NAME_DEFINITION.valueType(), key.type.name());
  }

  @Test
  public void testColumnsDefinition() {
    DumbSink sink = new DumbSink();
    ConfigDef.ConfigKey key =
        sink.config().configKeys().get(SettingDefinition.COLUMNS_DEFINITION.key());
    Assert.assertEquals(SettingDefinition.COLUMNS_DEFINITION.key(), key.name);
    Assert.assertEquals(SettingDefinition.COLUMNS_DEFINITION.orderInGroup(), key.orderInGroup);
    Assert.assertEquals(SettingDefinition.COLUMNS_DEFINITION.group(), key.group);
    // the TABLE is mapped to STRING
    Assert.assertEquals(SettingDefinition.Type.STRING.name(), key.type.name());
  }

  @Test
  public void testVersionDefinition() {
    DumbSink sink = new DumbSink();
    ConfigDef.ConfigKey key =
        sink.config().configKeys().get(SettingDefinition.VERSION_DEFINITION.key());
    Assert.assertEquals(SettingDefinition.VERSION_DEFINITION.key(), key.name);
    Assert.assertEquals(SettingDefinition.VERSION_DEFINITION.orderInGroup(), key.orderInGroup);
    Assert.assertEquals(SettingDefinition.VERSION_DEFINITION.group(), key.group);
    Assert.assertEquals(SettingDefinition.VERSION_DEFINITION.valueType(), key.type.name());
  }

  @Test
  public void testRevisionDefinition() {
    DumbSink sink = new DumbSink();
    ConfigDef.ConfigKey key =
        sink.config().configKeys().get(SettingDefinition.REVISION_DEFINITION.key());
    Assert.assertEquals(SettingDefinition.REVISION_DEFINITION.key(), key.name);
    Assert.assertEquals(SettingDefinition.REVISION_DEFINITION.orderInGroup(), key.orderInGroup);
    Assert.assertEquals(SettingDefinition.REVISION_DEFINITION.group(), key.group);
    Assert.assertEquals(SettingDefinition.REVISION_DEFINITION.valueType(), key.type.name());
  }

  @Test
  public void testAuthorDefinition() {
    DumbSink sink = new DumbSink();
    ConfigDef.ConfigKey key =
        sink.config().configKeys().get(SettingDefinition.AUTHOR_DEFINITION.key());
    Assert.assertEquals(SettingDefinition.AUTHOR_DEFINITION.key(), key.name);
    Assert.assertEquals(SettingDefinition.AUTHOR_DEFINITION.orderInGroup(), key.orderInGroup);
    Assert.assertEquals(SettingDefinition.AUTHOR_DEFINITION.group(), key.group);
    Assert.assertEquals(SettingDefinition.AUTHOR_DEFINITION.valueType(), key.type.name());
  }

  @Test
  public void testConnectorType() {
    DumbSink sink = new DumbSink();
    ConfigDef.ConfigKey key =
        sink.config().configKeys().get(SettingDefinition.KIND_DEFINITION.key());
    Assert.assertEquals(SettingDefinition.KIND_DEFINITION.key(), key.name);
    Assert.assertEquals(SettingDefinition.KIND_DEFINITION.orderInGroup(), key.orderInGroup);
    Assert.assertEquals(SettingDefinition.KIND_DEFINITION.group(), key.group);
    Assert.assertEquals(SettingDefinition.KIND_DEFINITION.valueType(), key.type.name());
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

  private static class SourceWithNullableSetting extends RowSourceConnector {
    private final String version;
    private final String revision;
    private final String author;

    SourceWithNullableSetting(String version, String revision, String author) {
      this.version = version;
      this.revision = revision;
      this.author = author;
    }

    @Override
    protected Class<? extends RowSourceTask> _taskClass() {
      return null;
    }

    @Override
    protected List<TaskConfig> _taskConfigs(int maxTasks) {
      return null;
    }

    @Override
    protected void _start(TaskConfig config) {}

    @Override
    protected void _stop() {}

    @Override
    protected ConnectorVersion _version() {
      return ConnectorVersion.builder().version(version).revision(revision).author(author).build();
    }
  }

  private static class SinkWithNullableSetting extends RowSinkConnector {
    private final String version;
    private final String revision;
    private final String author;

    SinkWithNullableSetting(String version, String revision, String author) {
      this.version = version;
      this.revision = revision;
      this.author = author;
    }

    @Override
    protected Class<? extends RowSinkTask> _taskClass() {
      return null;
    }

    @Override
    protected List<TaskConfig> _taskConfigs(int maxTasks) {
      return null;
    }

    @Override
    protected void _start(TaskConfig config) {}

    @Override
    protected void _stop() {}

    @Override
    protected ConnectorVersion _version() {
      return ConnectorVersion.builder().version(version).revision(revision).author(author).build();
    }
  }
}
