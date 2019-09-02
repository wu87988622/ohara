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

package com.island.ohara.kafka.connector.csv.source;

import com.island.ohara.common.util.CommonUtils;
import com.island.ohara.kafka.connector.RowSourceContext;
import com.island.ohara.kafka.connector.RowSourceRecord;
import com.island.ohara.kafka.connector.storage.Storage;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** An implementation of DataReader for CSV file */
public class CsvDataReader implements DataReader {
  private static final Logger LOG = LoggerFactory.getLogger(CsvDataReader.class);

  private final Storage storage;
  private final CsvSourceConfig config;
  private final RowSourceContext context;
  private final CsvOffsetCache offsetCache;

  public static CsvDataReader of(
      Storage storage, CsvSourceConfig config, RowSourceContext context) {
    return new CsvDataReader(storage, config, context);
  }

  public CsvDataReader(Storage storage, CsvSourceConfig config, RowSourceContext context) {
    this.storage = storage;
    this.config = config;
    this.context = context;
    this.offsetCache = new CsvOffsetCache();
  }

  public List<RowSourceRecord> read(Path file) {
    try {
      offsetCache.update(context, file.toString());
      CsvRecordConverter converter =
          new CsvRecordConverter.Builder()
              .path(file.toString())
              .topics(config.topics())
              .offsetCache(offsetCache)
              .schema(config.schema())
              .build();

      List<RowSourceRecord> records;
      try (BufferedReader reader =
          new BufferedReader(
              new InputStreamReader(
                  storage.open(file.toString()), Charset.forName(config.encode())))) {
        records = converter.convert(reader.lines());
      }

      handleCompletedFile(file.toString());
      return records;
    } catch (Exception e) {
      LOG.error("failed to handle " + file, e);
      handleErrorFile(file.toString());
      return Collections.emptyList();
    }
  }

  /**
   * Move the file to the completed folder, or delete it directly.
   *
   * @param path the file path
   */
  private void handleCompletedFile(String path) {
    if (config.completedFolder().isPresent()) {
      try {
        moveToAnotherFolder(path, config.completedFolder().get());
      } catch (Exception e) {
        throw new IllegalStateException(
            "failed to move " + path + " to " + config.completedFolder(), e);
      }
    } else {
      try {
        storage.delete(path);
      } catch (Exception e) {
        throw new IllegalStateException("failed to delete " + path, e);
      }
    }
  }

  /**
   * Move the file to the error folder. If any error occur, only log the error reason.
   *
   * @param path the file path
   */
  private void handleErrorFile(String path) {
    try {
      moveToAnotherFolder(path, config.errorFolder());
    } catch (Exception e) {
      LOG.error("failed to move " + path + " to " + config.errorFolder(), e);
    }
  }

  private void moveToAnotherFolder(String path, String targetFolder) {
    String fileName = Paths.get(path).getFileName().toString();
    String outputPath = Paths.get(targetFolder, fileName).toString();
    if (storage.exists(outputPath)) {
      String newPath = outputPath + "." + CommonUtils.uuid();
      if (storage.exists(newPath)) {
        throw new IllegalStateException("duplicate file? " + path);
      } else {
        storage.move(path, newPath);
      }
    } else {
      storage.move(path, outputPath);
    }
  }
}
