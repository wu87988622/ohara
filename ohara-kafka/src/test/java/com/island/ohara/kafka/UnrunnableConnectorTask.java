package com.island.ohara.kafka;

import com.island.ohara.kafka.connector.RowSourceRecord;
import com.island.ohara.kafka.connector.RowSourceTask;
import com.island.ohara.kafka.connector.TaskConfig;
import java.util.Collections;
import java.util.List;

public class UnrunnableConnectorTask extends RowSourceTask {
  @Override
  protected void _start(TaskConfig config) {}

  @Override
  protected void _stop() {}

  @Override
  protected List<RowSourceRecord> _poll() {
    return Collections.emptyList();
  }
}
