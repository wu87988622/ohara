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

import com.google.common.collect.Iterators;
import com.island.ohara.common.exception.OharaException;
import com.island.ohara.common.util.CommonUtils;
import com.island.ohara.kafka.connector.storage.LocalStorage;
import com.island.ohara.kafka.connector.storage.Storage;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.rules.TemporaryFolder;

public abstract class WithFakeStorage extends CsvSourceTestBase {
  protected static final Path ROOT_FOLDER = createTemporaryFolder();
  protected static final Path INPUT_FOLDER = Paths.get(ROOT_FOLDER.toString(), "input");
  protected static final Path COMPLETED_FOLDER = Paths.get(ROOT_FOLDER.toString(), "completed");
  protected static final Path ERROR_FOLDER = Paths.get(ROOT_FOLDER.toString(), "error");
  protected static final Path INPUT_FILE =
      Paths.get(INPUT_FOLDER.toString(), CommonUtils.randomString(5));

  protected Storage storage;

  @Override
  protected Map<String, String> createProps() {
    Map<String, String> props = super.createProps();
    props.put(CsvSourceConfig.INPUT_FOLDER_CONFIG, INPUT_FOLDER.toString());
    props.put(CsvSourceConfig.COMPLETED_FOLDER_CONFIG, COMPLETED_FOLDER.toString());
    props.put(CsvSourceConfig.ERROR_FOLDER_CONFIG, ERROR_FOLDER.toString());
    return props;
  }

  @Override
  protected void setup() {
    super.setup();

    storage = LocalStorage.of();
    cleanFolders();
    setupInputFile();
  }

  private static Path createTemporaryFolder() {
    try {
      TemporaryFolder folder = new TemporaryFolder();
      folder.create();
      return folder.getRoot().toPath();
    } catch (IOException e) {
      throw new OharaException(e);
    }
  }

  protected void verifyFileSizeInFolder(int expected, Path folder) {
    Assert.assertEquals(expected, Iterators.size(storage.list(folder.toString())));
  }

  private void cleanFolders() {
    storage.delete(INPUT_FOLDER.toString());
    storage.delete(COMPLETED_FOLDER.toString());
    storage.delete(ERROR_FOLDER.toString());
    storage.mkdirs(INPUT_FOLDER.toString());
    storage.mkdirs(COMPLETED_FOLDER.toString());
    storage.mkdirs(ERROR_FOLDER.toString());
  }

  protected void setupInputFile() {
    List<String> data = INPUT_DATA != null ? INPUT_DATA : setupInputData();

    try {
      if (storage.exists(INPUT_FILE.toString())) {
        storage.delete(INPUT_FILE.toString());
      }

      BufferedWriter writer =
          new BufferedWriter(new OutputStreamWriter(storage.create(INPUT_FILE.toString())));

      String header = SCHEMA.stream().map(column -> column.name()).collect(Collectors.joining(","));
      writer.append(header);
      writer.newLine();

      if (data != null && data.size() > 0) {
        for (String line : data) {
          writer.append(line);
          writer.newLine();
        }
      }
      writer.close();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
