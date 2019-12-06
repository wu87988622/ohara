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

package com.island.ohara.kafka.connector.csv.sink;

import static com.island.ohara.kafka.connector.csv.CsvConnectorDefinitions.FILE_ENCODE_DEFAULT;
import static com.island.ohara.kafka.connector.csv.CsvConnectorDefinitions.FILE_ENCODE_KEY;
import static com.island.ohara.kafka.connector.csv.CsvConnectorDefinitions.FILE_NEED_HEADER_DEFAULT;
import static com.island.ohara.kafka.connector.csv.CsvConnectorDefinitions.FILE_NEED_HEADER_KEY;
import static com.island.ohara.kafka.connector.csv.CsvConnectorDefinitions.FLUSH_SIZE_DEFAULT;
import static com.island.ohara.kafka.connector.csv.CsvConnectorDefinitions.FLUSH_SIZE_KEY;
import static com.island.ohara.kafka.connector.csv.CsvConnectorDefinitions.OUTPUT_FOLDER_KEY;
import static com.island.ohara.kafka.connector.csv.CsvConnectorDefinitions.ROTATE_INTERVAL_MS_DEFAULT;
import static com.island.ohara.kafka.connector.csv.CsvConnectorDefinitions.ROTATE_INTERVAL_MS_KEY;

import com.island.ohara.common.annotations.VisibleForTesting;
import com.island.ohara.common.data.Column;
import com.island.ohara.kafka.connector.TaskSetting;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** This class is used to define the configuration of CsvSinkTask. */
public class CsvSinkConfig {
  private final int flushSize;
  private final long rotateIntervalMs;
  private final String outputFolder;
  private final String encode;
  private final boolean needHeader;
  private final List<Column> schema;

  private CsvSinkConfig(Builder builder) {
    this.outputFolder = builder.outputFolder;
    this.flushSize = builder.flushSize;
    this.rotateIntervalMs = builder.rotateIntervalMs;
    this.encode = builder.encode;
    this.needHeader = builder.needHeader;
    this.schema = builder.schema;
  }

  public int flushSize() {
    return flushSize;
  }

  public long rotateIntervalMs() {
    return rotateIntervalMs;
  }

  public String outputFolder() {
    return outputFolder;
  }

  public String encode() {
    return encode;
  }

  public boolean needHeader() {
    return needHeader;
  }

  public List<Column> schema() {
    return schema;
  }

  /**
   * Creates a CsvSinkConfig based on raw input.
   *
   * @param props raw input
   * @param schema the schema
   * @return CsvSinkConfig
   */
  public static CsvSinkConfig of(Map<String, String> props, List<Column> schema) {
    return of(TaskSetting.of(props), schema);
  }

  /**
   * Create a CsvSinkConfig based on task setting definitions. This method is used by CsvSinkTask.
   *
   * @param setting setting definitions
   * @param schema the schema
   * @return an object of CsvSinkConfig
   */
  public static CsvSinkConfig of(TaskSetting setting, List<Column> schema) {
    Builder builder = new Builder();

    Optional<String> outputFolder = setting.stringOption(OUTPUT_FOLDER_KEY);
    if (outputFolder.isPresent()) {
      builder.outputFolder(outputFolder.get());
    }

    Optional<Integer> flushSize = setting.intOption(FLUSH_SIZE_KEY);
    if (flushSize.isPresent()) {
      builder.flushSize(flushSize.get());
    }

    Optional<Long> rotateIntervalMs = setting.longOption(ROTATE_INTERVAL_MS_KEY);
    if (rotateIntervalMs.isPresent()) {
      builder.rotateIntervalMs(rotateIntervalMs.get());
    }

    Optional<Boolean> needHeader = setting.booleanOption(FILE_NEED_HEADER_KEY);
    if (needHeader.isPresent()) {
      builder.needHeader(needHeader.get());
    }

    Optional<String> encode = setting.stringOption(FILE_ENCODE_KEY);
    if (encode.isPresent()) {
      builder.encode(encode.get());
    }

    if (schema != null) {
      builder.schema(schema);
    } else {
      builder.schema(setting.columns());
    }

    return builder.build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder implements com.island.ohara.common.pattern.Builder<CsvSinkConfig> {
    private String outputFolder;
    private int flushSize = FLUSH_SIZE_DEFAULT;
    private long rotateIntervalMs = ROTATE_INTERVAL_MS_DEFAULT;
    private boolean needHeader = FILE_NEED_HEADER_DEFAULT;
    private String encode = FILE_ENCODE_DEFAULT;
    private List<Column> schema;

    public Builder outputFolder(String val) {
      outputFolder = val;
      return this;
    }

    @com.island.ohara.common.annotations.Optional("default is " + FLUSH_SIZE_DEFAULT)
    public Builder flushSize(int val) {
      flushSize = val;
      return this;
    }

    @com.island.ohara.common.annotations.Optional("default is " + ROTATE_INTERVAL_MS_DEFAULT)
    public Builder rotateIntervalMs(long val) {
      rotateIntervalMs = val;
      return this;
    }

    @com.island.ohara.common.annotations.Optional("default is " + FILE_ENCODE_DEFAULT)
    public Builder encode(String val) {
      encode = val;
      return this;
    }

    @com.island.ohara.common.annotations.Optional("default is " + FILE_NEED_HEADER_DEFAULT)
    public Builder needHeader(boolean val) {
      needHeader = val;
      return this;
    }

    public Builder schema(List<Column> val) {
      schema = val;
      return this;
    }

    @Override
    public CsvSinkConfig build() {
      Objects.requireNonNull(outputFolder);
      return new CsvSinkConfig(this);
    }
  }

  @VisibleForTesting
  public Map<String, String> toProps() {
    Map<String, String> props = new HashMap<>();
    props.put(OUTPUT_FOLDER_KEY, outputFolder);
    props.put(FLUSH_SIZE_KEY, String.valueOf(flushSize));
    props.put(ROTATE_INTERVAL_MS_KEY, String.valueOf(rotateIntervalMs));
    props.put(FILE_NEED_HEADER_KEY, String.valueOf(needHeader));
    props.put(FILE_ENCODE_KEY, encode);
    return props;
  }
}
