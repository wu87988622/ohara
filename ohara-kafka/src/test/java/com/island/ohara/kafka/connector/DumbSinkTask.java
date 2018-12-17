package com.island.ohara.kafka.connector;

import java.util.List;

public class DumbSinkTask extends RowSinkTask {
  @Override
  protected void _start(TaskConfig config) {}

  @Override
  protected void _stop() {}

  @Override
  protected void _put(List<RowSinkRecord> records) {}
}
