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
import com.island.ohara.kafka.connector.json.ConnectorFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DumbSource extends RowSourceConnector {
  private final List<Column> columns =
      Arrays.asList(
          Column.builder().name("cf0").dataType(DataType.BOOLEAN).order(0).build(),
          Column.builder().name("cf1").dataType(DataType.BOOLEAN).order(1).build());

  @Override
  protected Class<? extends RowSourceTask> _taskClass() {
    return DumbSourceTask.class;
  }

  @Override
  protected List<TaskConfig> _taskConfigs(int maxTasks) {
    return Collections.singletonList(
        ConnectorFormatter.of().name("test").topicName("topic").columns(columns).taskConfig());
  }

  @Override
  protected void _start(TaskConfig config) {}

  @Override
  protected void _stop() {}
}
