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

import com.island.ohara.common.setting.SettingDef;
import com.island.ohara.kafka.connector.RowSinkConnector;
import com.island.ohara.kafka.connector.TaskSetting;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * CsvSinkConnector inherits from {@link RowSinkConnector}. It also have {@link
 * RowSinkConnector#_start(TaskSetting)}`, {@link RowSinkConnector#_stop()}`, {@link
 * RowSinkConnector#_taskClass()}`, {@link RowSinkConnector#_taskSettings(int)}`. The main
 * difference between CsvSinkConnector and RowSinkConnector is that CsvSinkConnector already has
 * some default definitions. As follows:
 *
 * <ul>
 *   <li>TOPICS_DIR_DEFINITION: Read csv data from topic and then write to this folder
 *   <li>FLUSH_SIZE_DEFINITION: Number of records write to store before invoking file commits
 *   <li>ROTATE_INTERVAL_MS_DEFINITION: Commit file time
 *   <li>FILE_NEED_HEADER_DEFINITION: File need header for flush data
 *   <li>FILE_ENCODE_DEFINITION: File encode for write to file
 * </ul>
 */
public abstract class CsvSinkConnector extends RowSinkConnector implements CsvConnector {
  public static final SettingDef TOPICS_DIR_DEFINITION =
      SettingDef.builder()
          .displayName("Output Folder")
          .documentation("Read csv data from topic and then write to this folder")
          .valueType(SettingDef.Type.STRING)
          .key(TOPICS_DIR_CONFIG)
          .build();

  public static final SettingDef FLUSH_SIZE_DEFINITION =
      SettingDef.builder()
          .displayName("Flush Size")
          .documentation("Number of records write to store before invoking file commits")
          .valueType(SettingDef.Type.INT)
          .key(FLUSH_SIZE_CONFIG)
          .optional(FLUSH_SIZE_DEFAULT)
          .build();

  public static final SettingDef ROTATE_INTERVAL_MS_DEFINITION =
      SettingDef.builder()
          .displayName("Rotate Interval(MS)")
          .documentation("Commit file time")
          .valueType(SettingDef.Type.LONG)
          .key(ROTATE_INTERVAL_MS_CONFIG)
          .optional(ROTATE_INTERVAL_MS_DEFAULT)
          .build();

  public static final SettingDef FILE_NEED_HEADER_DEFINITION =
      SettingDef.builder()
          .displayName("File Need Header")
          .documentation("File need header for flush data")
          .valueType(SettingDef.Type.BOOLEAN)
          .key(FILE_NEED_HEADER_CONFIG)
          .optional(FILE_NEED_HEADER_DEFAULT)
          .build();

  public static final SettingDef FILE_ENCODE_DEFINITION =
      SettingDef.builder()
          .displayName("File Encode")
          .documentation("File encode for write to file")
          .valueType(SettingDef.Type.STRING)
          .key(FILE_ENCODE_CONFIG)
          .optional(FILE_ENCODE_DEFAULT)
          .build();

  /** the default definitions for csv sink connector. */
  public static final List<SettingDef> SINK_DEFINITIONS_DEFAULT =
      Arrays.asList(
          TOPICS_DIR_DEFINITION,
          FLUSH_SIZE_DEFINITION,
          ROTATE_INTERVAL_MS_DEFINITION,
          FILE_NEED_HEADER_DEFINITION,
          FILE_ENCODE_DEFINITION);

  @Override
  public List<SettingDef> definitions() {
    return Stream.of(SINK_DEFINITIONS_DEFAULT, super.definitions())
        .flatMap(List::stream)
        .collect(Collectors.toList());
  }
}
