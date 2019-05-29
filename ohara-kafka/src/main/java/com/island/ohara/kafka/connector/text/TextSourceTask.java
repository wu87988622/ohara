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

package com.island.ohara.kafka.connector.text;

import com.island.ohara.common.util.Releasable;
import com.island.ohara.kafka.connector.RowSourceRecord;
import com.island.ohara.kafka.connector.RowSourceTask;
import com.island.ohara.kafka.connector.TaskSetting;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TextSourceTask is a Kafka Connect SourceTask implementation that reads from Text files (ex: CVS,
 * JSON, XML) and generates Kafka Connect records.
 */
public abstract class TextSourceTask extends RowSourceTask {
  private static final Logger log = LoggerFactory.getLogger(TextSourceTask.class);

  private TextSourceConverterFactory converterFactory;
  private TextFileSystem fileSystem;

  /**
   * Return the TextSourceConverterFactory for this connector
   *
   * @param config configuration settings
   * @return the TextSourceConverterFactory for this connector
   */
  public abstract TextSourceConverterFactory getConverterFactory(TaskSetting config);

  /**
   * Return the TextFileSystem for this connector
   *
   * @param config configuration config
   * @return the TextFileSystem for this connector
   */
  public abstract TextFileSystem getFileSystem(TaskSetting config);

  @Override
  protected void _start(TaskSetting config) {
    converterFactory = Objects.requireNonNull(getConverterFactory(config));
    fileSystem = Objects.requireNonNull(getFileSystem(config));
  }

  @Override
  protected List<RowSourceRecord> _poll() {
    Optional<String> inputFile = fileSystem.listInputFiles().stream().findFirst();

    if (inputFile.isPresent()) {
      String path = inputFile.get();
      try {
        TextSourceConverter converter = converterFactory.newConverter(rowContext, path);
        List<RowSourceRecord> records;
        try (InputStreamReader reader = fileSystem.createReader(path)) {
          records = converter.convert(reader);
        }
        if (records.isEmpty()) fileSystem.handleCompletedFile(path);
        return records;
      } catch (Exception e) {
        log.error("failed to handle " + path, e);
        fileSystem.handleErrorFile(path);
        return Collections.emptyList();
      }
    }

    return Collections.emptyList();
  }

  @Override
  protected void _stop() {
    Releasable.close(fileSystem);
  }
}
