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

package oharastream.ohara.kafka.connector.csv;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import oharastream.ohara.common.annotations.VisibleForTesting;
import oharastream.ohara.common.util.Releasable;
import oharastream.ohara.kafka.connector.RowSourceRecord;
import oharastream.ohara.kafka.connector.RowSourceTask;
import oharastream.ohara.kafka.connector.TaskSetting;
import oharastream.ohara.kafka.connector.csv.source.CsvDataReader;
import oharastream.ohara.kafka.connector.csv.source.CsvSourceConfig;
import oharastream.ohara.kafka.connector.csv.source.DataReader;
import oharastream.ohara.kafka.connector.storage.FileSystem;

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
  public abstract FileSystem fileSystem(TaskSetting config);

  @Override
  public final void run(TaskSetting setting) {
    fs = fileSystem(setting);
    config = CsvSourceConfig.of(setting);
    dataReader = CsvDataReader.of(fs, config, rowContext);
  }

  @Override
  public final List<RowSourceRecord> pollRecords() {
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
  public final void terminate() {
    Releasable.close(fs);
  }

  @VisibleForTesting
  public DataReader getDataReader() {
    return dataReader;
  }
}
