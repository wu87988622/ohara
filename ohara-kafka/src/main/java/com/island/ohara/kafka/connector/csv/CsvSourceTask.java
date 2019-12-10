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

import com.island.ohara.common.annotations.VisibleForTesting;
import com.island.ohara.common.util.Releasable;
import com.island.ohara.kafka.connector.RowSourceRecord;
import com.island.ohara.kafka.connector.RowSourceTask;
import com.island.ohara.kafka.connector.TaskSetting;
import com.island.ohara.kafka.connector.csv.source.*;
import com.island.ohara.kafka.connector.storage.FileSystem;
import java.nio.file.Paths;
import java.util.*;

/**
 * CsvSourceTask moveFile files from file system to Kafka topics. The file format must be csv file,
 * and element in same line must be separated by comma. The offset is (path, line index). It means
 * each line is stored as a "message" in connector topic. For example: a file having 100 lines has
 * 100 message in connector topic.
 */
public abstract class CsvSourceTask extends RowSourceTask {
  private CsvSourceConfig config;
  private DataReader dataReader;
  private FileSystem fs;

  /**
   * Return the file system for this connector
   *
   * @param config initial configuration
   * @return a FileSystem implementation
   */
  public abstract FileSystem _fileSystem(TaskSetting config);

  @Override
  public final void _start(TaskSetting setting) {
    fs = _fileSystem(setting);
    config = CsvSourceConfig.of(setting);
    dataReader = CsvDataReader.of(fs, config, rowContext);
  }

  @Override
  public final List<RowSourceRecord> _poll() {
    Iterator<String> fileNames = fs.listFileNames(config.inputFolder());
    if (fileNames.hasNext()) {
      String fileName = fileNames.next();
      // Avoid more than one Task processing the same file
      if (fileName.hashCode() % config.total() == config.hash()) {
        String path = Paths.get(config.inputFolder(), fileName).toString();
        return dataReader.read(path);
      }
    }
    return Collections.emptyList();
  }

  @Override
  public final void _stop() {
    Releasable.close(fs);
  }

  @VisibleForTesting
  public DataReader getDataReader() {
    return dataReader;
  }
}
