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
