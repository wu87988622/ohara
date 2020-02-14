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

import com.island.ohara.common.setting.SettingDef;
import com.island.ohara.common.setting.WithDefinitions;
import com.island.ohara.kafka.connector.RowSourceConnector;
import com.island.ohara.kafka.connector.RowSourceTask;
import com.island.ohara.kafka.connector.TaskSetting;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SourceWithNullableSetting extends RowSourceConnector {
  private final String version;
  private final String revision;
  private final String author;

  /** open to kafka broker. the inaccessible connector can break the construction of broker. */
  public SourceWithNullableSetting() {
    this("unknown", "unknown", "unknown");
  }

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
  protected List<TaskSetting> _taskSettings(int maxTasks) {
    return null;
  }

  @Override
  protected void _start(TaskSetting config) {}

  @Override
  protected void _stop() {}

  @Override
  public Map<String, SettingDef> customSettingDefinitions() {
    return Stream.of(
            WithDefinitions.authorDefinition(author),
            WithDefinitions.versionDefinition(version),
            WithDefinitions.revisionDefinition(revision))
        .collect(Collectors.toMap(SettingDef::key, Function.identity()));
  }
}
