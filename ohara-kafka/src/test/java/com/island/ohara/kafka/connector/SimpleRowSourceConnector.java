package com.island.ohara.kafka.connector;

import java.util.Collections;
import java.util.List;

/** Used for testing. */
public class SimpleRowSourceConnector extends RowSourceConnector {

  private TaskConfig config = null;

  @Override
  protected Class<? extends RowSourceTask> _taskClass() {
    return SimpleRowSourceTask.class;
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
