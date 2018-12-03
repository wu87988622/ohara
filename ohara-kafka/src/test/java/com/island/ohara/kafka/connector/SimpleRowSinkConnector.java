package com.island.ohara.kafka.connector;

import static com.island.ohara.kafka.connector.Constants.BROKER;
import static com.island.ohara.kafka.connector.Constants.OUTPUT;

import java.util.Collections;
import java.util.List;

/** Used for testing. */
public class SimpleRowSinkConnector extends RowSinkConnector {
  @Override
  protected void _start(TaskConfig props) {
    this.config = props;
    // check the option
    this.config.options().get(OUTPUT);
    this.config.options().get(BROKER);
  }

  @Override
  protected void _stop() {}

  @Override
  protected Class<? extends RowSinkTask> _taskClass() {

    return SimpleRowSinkTask.class;
  }

  @Override
  protected List<TaskConfig> _taskConfigs(int maxTasks) {
    return Collections.nCopies(maxTasks, config);
  }

  private TaskConfig config = null;
}
