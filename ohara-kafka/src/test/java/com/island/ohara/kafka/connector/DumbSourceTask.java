package com.island.ohara.kafka.connector;

import java.util.Collections;
import java.util.List;

public class DumbSourceTask extends RowSourceTask {
  @Override
  protected void _start(TaskConfig config) {}

  @Override
  protected void _stop() {}

  @Override
  protected List<RowSourceRecord> _poll() {
    return Collections.emptyList();
  }
}
