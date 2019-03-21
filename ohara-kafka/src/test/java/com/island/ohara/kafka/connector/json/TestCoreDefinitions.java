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

  @Test
  public void testConnectorType() {
    DumbSink sink = new DumbSink();
    ConfigDef.ConfigKey key = sink.config().configKeys().get(ConnectorFormatter.CONNECTOR_TYPE_KEY);
    Assert.assertEquals(ConnectorFormatter.CONNECTOR_TYPE_KEY, key.name);
    Assert.assertEquals(ConnectorFormatter.CONNECTOR_TYPE_KEY_ORDER, key.orderInGroup);
    Assert.assertEquals(SettingDefinition.CORE_GROUP, key.group);
    Assert.assertEquals(SettingDefinition.Type.STRING.name(), key.type.name());
  }

  @Test(expected = NullPointerException.class)
  public void nullVersionInSource() {
    new SourceWithNullableSetting(
            null,
            CommonUtils.randomString(),
            CommonUtils.randomString(),
            CommonUtils.randomString())
        .config();
  }

  @Test(expected = NullPointerException.class)
  public void nullRevisionInSource() {
    new SourceWithNullableSetting(
            CommonUtils.randomString(),
            null,
            CommonUtils.randomString(),
            CommonUtils.randomString())
        .config();
  }

  @Test(expected = NullPointerException.class)
  public void nullAuthorInSource() {
    new SourceWithNullableSetting(
            CommonUtils.randomString(),
            CommonUtils.randomString(),
            null,
            CommonUtils.randomString())
        .config();
  }

  @Test(expected = NullPointerException.class)
  public void nullConnectorTypeInSource() {
    new SourceWithNullableSetting(
            CommonUtils.randomString(),
            CommonUtils.randomString(),
            CommonUtils.randomString(),
            null)
        .config();
  }

  @Test(expected = NullPointerException.class)
  public void nullVersionInSink() {
    new SourceWithNullableSetting(
            null,
            CommonUtils.randomString(),
            CommonUtils.randomString(),
            CommonUtils.randomString())
        .config();
  }

  @Test(expected = NullPointerException.class)
  public void nullRevisionInSink() {
    new SinkWithNullableSetting(
            CommonUtils.randomString(),
            null,
            CommonUtils.randomString(),
            CommonUtils.randomString())
        .config();
  }

  @Test(expected = NullPointerException.class)
  public void nullAuthorInSink() {
    new SinkWithNullableSetting(
            CommonUtils.randomString(),
            CommonUtils.randomString(),
            null,
            CommonUtils.randomString())
        .config();
  }

  @Test(expected = NullPointerException.class)
  public void nullConnectorTypeInSink() {
    new SinkWithNullableSetting(
            CommonUtils.randomString(),
            CommonUtils.randomString(),
            CommonUtils.randomString(),
            null)
        .config();
  }

  private static class SourceWithNullableSetting extends RowSourceConnector {
    private final String version;
    private final String revision;
    private final String author;
    private final String connectorType;

    SourceWithNullableSetting(
        String version, String revision, String author, String connectorType) {
      this.version = version;
      this.revision = revision;
      this.author = author;
      this.connectorType = connectorType;
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
    protected String _version() {
      return version;
    }

    @Override
    protected String author() {
      return author;
    }

    @Override
    protected String revision() {
      return revision;
    }

    @Override
    protected String typeName() {
      return connectorType;
    }
  }

  private static class SinkWithNullableSetting extends RowSinkConnector {
    private final String version;
    private final String revision;
    private final String author;
    private final String connectorType;

    SinkWithNullableSetting(String version, String revision, String author, String connectorType) {
      this.version = version;
      this.revision = revision;
      this.author = author;
      this.connectorType = connectorType;
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
    protected String _version() {
      return version;
    }

    @Override
    protected String author() {
      return author;
    }

    @Override
    protected String revision() {
      return revision;
    }

    @Override
    protected String typeName() {
      return connectorType;
    }
  }
}
