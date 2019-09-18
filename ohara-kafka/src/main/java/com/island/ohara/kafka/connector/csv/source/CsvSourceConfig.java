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

import com.island.ohara.common.annotations.VisibleForTesting;
import com.island.ohara.common.data.Column;
import com.island.ohara.common.util.CommonUtils;
import com.island.ohara.kafka.connector.TaskSetting;
import com.island.ohara.kafka.connector.csv.CsvConnector;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class CsvSourceConfig implements CsvConnector {
  private int total;
  private int hash;
  private String inputFolder;
  private Optional<String> completedFolder;
  private String errorFolder;
  private String encode;
  private List<String> topics;
  private List<Column> schema;

  public int total() {
    return total;
  }

  public int hash() {
    return hash;
  }

  public String inputFolder() {
    return inputFolder;
  }

  public Optional<String> completedFolder() {
    return completedFolder;
  }

  public String errorFolder() {
    return errorFolder;
  }

  public String encode() {
    return encode;
  }

  public List<String> topics() {
    return topics;
  }

  @VisibleForTesting
  public void topics(List<String> topics) {
    this.topics = topics;
  }

  public List<Column> schema() {
    return schema;
  }

  @VisibleForTesting
  public void schema(List<Column> schema) {
    this.schema = schema;
  }

  public static CsvSourceConfig.Builder builder() {
    return new CsvSourceConfig.Builder();
  }

  public static class Builder implements com.island.ohara.common.pattern.Builder<CsvSourceConfig> {
    private int total;
    private int hash;
    private String inputFolder;
    private Optional<String> completedFolder;
    private String errorFolder;
    private String encode = FILE_ENCODE_DEFAULT;
    private List<String> topics;
    private List<Column> schema;

    public CsvSourceConfig.Builder total(int val) {
      total = val;
      return this;
    }

    public CsvSourceConfig.Builder hash(int val) {
      hash = val;
      return this;
    }

    public CsvSourceConfig.Builder inputFolder(String val) {
      inputFolder = val;
      return this;
    }

    public CsvSourceConfig.Builder completedFolder(Optional<String> val) {
      completedFolder = val;
      return this;
    }

    public CsvSourceConfig.Builder errorFolder(String val) {
      errorFolder = val;
      return this;
    }

    @com.island.ohara.common.annotations.Optional("default is " + FILE_ENCODE_DEFAULT)
    public CsvSourceConfig.Builder encode(String val) {
      encode = val;
      return this;
    }

    public CsvSourceConfig.Builder topics(List<String> val) {
      topics = val;
      return this;
    }

    public CsvSourceConfig.Builder schema(List<Column> val) {
      schema = val;
      return this;
    }

    @Override
    public CsvSourceConfig build() {
      Objects.requireNonNull(inputFolder);
      Objects.requireNonNull(errorFolder);
      CommonUtils.requireNonEmpty(topics);
      return new CsvSourceConfig(this);
    }
  }

  private CsvSourceConfig(CsvSourceConfig.Builder builder) {
    this.total = builder.total;
    this.hash = builder.hash;
    this.inputFolder = builder.inputFolder;
    this.completedFolder = builder.completedFolder;
    this.errorFolder = builder.errorFolder;
    this.encode = builder.encode;
    this.topics = builder.topics;
    this.schema = builder.schema;
  }

  @VisibleForTesting
  public static CsvSourceConfig of(Map<String, String> props, List<Column> schema) {
    return of(TaskSetting.of(props), schema);
  }

  public static CsvSourceConfig of(TaskSetting setting) {
    return of(setting, null);
  }

  public static CsvSourceConfig of(TaskSetting setting, List<Column> schema) {
    CsvSourceConfig.Builder builder = new CsvSourceConfig.Builder();

    builder.total(setting.intValue(TASK_TOTAL_KEY));
    builder.hash(setting.intValue(TASK_HASH_KEY));

    Optional<String> inputFolder = setting.stringOption(INPUT_FOLDER_KEY);
    if (inputFolder.isPresent()) {
      builder.inputFolder(inputFolder.get());
    }

    builder.completedFolder(setting.stringOption(COMPLETED_FOLDER_KEY));

    Optional<String> errorFolder = setting.stringOption(ERROR_FOLDER_KEY);
    if (errorFolder.isPresent()) {
      builder.errorFolder(errorFolder.get());
    }

    Optional<String> encode = setting.stringOption(FILE_ENCODE_KEY);
    if (encode.isPresent()) {
      builder.encode(encode.get());
    }

    builder.topics(setting.topicNames());

    if (schema != null) {
      builder.schema(schema);
    } else {
      builder.schema(setting.columns());
    }

    return builder.build();
  }
}
