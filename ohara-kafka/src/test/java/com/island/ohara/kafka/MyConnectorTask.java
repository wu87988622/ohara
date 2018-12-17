package com.island.ohara.kafka;

import com.island.ohara.kafka.connector.RowSourceRecord;
import com.island.ohara.kafka.connector.RowSourceTask;
import com.island.ohara.kafka.connector.TaskConfig;
import java.util.Collections;
import java.util.List;

public class MyConnectorTask extends RowSourceTask {
  private long lastSent = 0;
  private String topicName;

  @Override
  protected void _start(TaskConfig config) {
    this.topicName = config.topics().get(0);
  }

  @Override
  protected void _stop() {}

  @Override
  protected List<RowSourceRecord> _poll() {
    long current = System.currentTimeMillis();
    if (current - lastSent >= 1000) {
      lastSent = current;
      return Collections.singletonList(RowSourceRecord.of(topicName, TestConnectorClient.ROW));
    } else return Collections.emptyList();
  }
}
