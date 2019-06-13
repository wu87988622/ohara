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

public class DumbSink extends RowSinkConnector {
  private final List<Column> columns =
      Arrays.asList(
          Column.builder().name("cf0").dataType(DataType.BOOLEAN).order(0).build(),
          Column.builder().name("cf1").dataType(DataType.BOOLEAN).order(1).build());

  @Override
  protected void _start(TaskSetting config) {}

  @Override
  protected void _stop() {}

  @Override
  protected Class<? extends RowSinkTask> _taskClass() {
    return DumbSinkTask.class;
  }

  @Override
  protected List<TaskSetting> _taskSettings(int maxTasks) {
    return Collections.singletonList(
        TaskSetting.of(
            ConnectorFormatter.of().name("test").topicName("topic").columns(columns).raw()));
  }
}
