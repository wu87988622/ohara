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

package com.island.ohara.kafka;

import com.island.ohara.kafka.connector.RowSourceConnector;
import com.island.ohara.kafka.connector.RowSourceTask;
import com.island.ohara.kafka.connector.TaskConfig;
import java.util.List;

public class UnrunnableConnector extends RowSourceConnector {
  @Override
  protected Class<? extends RowSourceTask> _taskClass() {
    return UnrunnableConnectorTask.class;
  }

  @Override
  protected List<TaskConfig> _taskConfigs(int maxTasks) {
    throw new IllegalArgumentException("This is an unrunnable connector");
  }

  @Override
  protected void _start(TaskConfig config) {
    throw new IllegalArgumentException("This is an unrunnable connector");
  }

  @Override
  protected void _stop() {}
}
