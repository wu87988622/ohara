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

import com.island.ohara.common.util.VersionUtils;
import com.island.ohara.kafka.connector.json.ConnectorFormatter;
import com.island.ohara.kafka.connector.json.SettingDefinition;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.kafka.common.config.Config;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.ConnectorContext;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.sink.SinkConnector;

/** A wrap to SinkConnector. Currently, only Task is replaced by ohara object - RowSinkTask */
public abstract class RowSinkConnector extends SinkConnector {

  /**
   * Start this Connector. This method will only be called on a clean Connector, i.e. it has either
   * just been instantiated and initialized or _stop() has been invoked.
   *
   * @param config configuration settings
   */
  protected abstract void _start(TaskConfig config);

  /** stop this connector */
  protected abstract void _stop();

  /**
   * Returns the RowSinkTask implementation for this Connector.
   *
   * @return a RowSinkTask class
   */
  protected abstract Class<? extends RowSinkTask> _taskClass();

  /**
   * Return the settings for source task. NOTED: It is illegal to assign different topics to
   * RowSinkTask
   *
   * @param maxTasks number of tasks for this connector
   * @return the settings for each tasks
   */
  protected abstract List<TaskConfig> _taskConfigs(int maxTasks);

  /**
   * Define the configuration for the connector.
   *
   * @return The ConfigDef for this connector.
   */
  protected List<SettingDefinition> definitions() {
    return Collections.emptyList();
  }

  /**
   * Get the version from this connector.
   *
   * @return the version, formatted as a String
   */
  protected String _version() {
    return VersionUtils.VERSION;
  }

  /** @return revision of git commit */
  protected String revision() {
    return VersionUtils.REVISION;
  }

  /** @return author or your boss */
  protected String author() {
    return "you";
  }

  // -------------------------------------------------[WRAPPED]-------------------------------------------------//
  /** We take over this method to disable user to use java collection. */
  @Override
  public final List<Map<String, String>> taskConfigs(int maxTasks) {
    return _taskConfigs(maxTasks).stream().map(TaskConfig::raw).collect(Collectors.toList());
  }

  @Override
  public final Class<? extends Task> taskClass() {
    return _taskClass();
  }

  @Override
  public final void start(Map<String, String> props) {
    _start(TaskConfig.of(new HashMap<>(props)));
  }

  @Override
  public final void stop() {
    _stop();
  }

  @Override
  public final ConfigDef config() {
    return ConnectorFormatter.toConfigDef(
        Stream.of(definitions(), SettingDefinition.DEFINITIONS_DEFAULT)
            .flatMap(List::stream)
            .collect(Collectors.toList()),
        _version(),
        revision(),
        author());
  }

  @Override
  public final String version() {
    return _version();
  }

  // -------------------------------------------------[UN-OVERRIDE]-------------------------------------------------//
  @Override
  public final void initialize(ConnectorContext ctx) {
    super.initialize(ctx);
  }

  @Override
  public final void initialize(ConnectorContext ctx, List<Map<String, String>> taskConfigs) {
    super.initialize(ctx, taskConfigs);
  }

  @Override
  public final void reconfigure(Map<String, String> props) {
    super.reconfigure(props);
  }

  @Override
  public final Config validate(Map<String, String> connectorConfigs) {
    return super.validate(connectorConfigs);
  }
}
