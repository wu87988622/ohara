package com.island.ohara.kafka;

import com.island.ohara.kafka.connector.RowSourceConnector;
import com.island.ohara.kafka.connector.RowSourceTask;
import com.island.ohara.kafka.connector.TaskConfig;
import java.util.Collections;
import java.util.List;

public class MyConnector extends RowSourceConnector {
  private TaskConfig config;

  @Override
  protected Class<? extends RowSourceTask> _taskClass() {
    return MyConnectorTask.class;
  }

  @Override
  protected List<TaskConfig> _taskConfigs(int maxTasks) {
    return Collections.nCopies(maxTasks, config);
  }

  @Override
  protected void _start(TaskConfig config) {
    this.config = config;
  }

  @Override
  protected void _stop() {}
}
