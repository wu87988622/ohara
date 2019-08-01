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

package com.island.ohara.kafka.connector.csv;

import com.island.ohara.kafka.connector.*;
import com.island.ohara.kafka.connector.csv.sink.CsvDataWriter;
import com.island.ohara.kafka.connector.storage.Storage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CsvSinkTask ia a Ohara RowSinkTask wrapper. It used to convert the RowSinkRecord to CSV files.
 * Ohara developers should extend this class rather than RowSinkTask in order to let the conversion
 * from RowSinkRecord to CSV files work easily.
 */
public abstract class CsvSinkTask extends RowSinkTask {
  private static final Logger log = LoggerFactory.getLogger(CsvSinkTask.class);

  private CsvSinkConfig config;

  private Storage storage;

  private DataWriter writer;

  /**
   * Returns a configuration for CsvSinkTask.
   *
   * @param setting initial settings
   * @return a configuration for Task
   */
  public abstract CsvSinkConfig getConfig(TaskSetting setting);

  /**
   * Returns the Storage implementation for this Task.
   *
   * @param setting initial settings
   * @return a Storage instance
   */
  public abstract Storage getStorage(TaskSetting setting);

  @Override
  protected void _start(TaskSetting setting) {
    config = Objects.requireNonNull(getConfig(setting));
    storage = Objects.requireNonNull(getStorage(setting));
    writer = new CsvDataWriter(config, rowContext, storage);
  }

  @Override
  protected void _open(List<TopicPartition> partitions) {
    writer.attach(partitions);
  }

  @Override
  protected void _put(List<RowSinkRecord> records) {
    writer.write(records);
  }

  @Override
  public Map<TopicPartition, TopicOffset> _preCommit(Map<TopicPartition, TopicOffset> offsets) {
    Map<TopicPartition, TopicOffset> offsetsToCommit = new HashMap<>();

    for (Map.Entry<TopicPartition, Long> entry : writer.getCommittedOffsetsAndReset().entrySet()) {
      log.debug(
          "Found last committed offset {} for topic partition {}",
          entry.getValue(),
          entry.getKey());
      offsetsToCommit.put(entry.getKey(), new TopicOffset(null, entry.getValue()));
    }

    log.debug("Returning committed offsets {}", offsetsToCommit);
    return offsetsToCommit;
  }

  @Override
  protected void _close(List<TopicPartition> partitions) {
    if (writer != null) {
      writer.detach(partitions);
    }
  }

  @Override
  protected void _stop() {
    if (writer != null) {
      writer.close();
    }
  }
}
