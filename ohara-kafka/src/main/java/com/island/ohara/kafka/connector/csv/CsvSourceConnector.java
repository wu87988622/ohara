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
import com.island.ohara.kafka.connector.RowSourceConnector;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * A wrap to RowSourceConnector. The difference between CsvSourceConnector and RowSourceConnector is
 * that CsvSourceConnector already has some default definitions, as follows:
 *
 * <ul>
 *   <li>INPUT_FOLDER_DEFINITION: Connector will load csv file from this folder
 *   <li>COMPLETED_FOLDER_DEFINITION: This folder is used to store the completed files
 *   <li>ERROR_FOLDER_DEFINITION: This folder is used to keep the invalid files
 *   <li>FILE_ENCODE_DEFINITION: File encode for write to file
 * </ul>
 */
public abstract class CsvSourceConnector extends RowSourceConnector {
  @Override
  public List<Map<String, String>> taskConfigs(int maxTasks) {
    List<Map<String, String>> taskConfigs = super.taskConfigs(maxTasks);
    return IntStream.range(0, maxTasks)
        .mapToObj(
            index -> {
              Map<String, String> taskConfig = new HashMap<>(taskConfigs.get(index));
              taskConfig.put(CsvConnector.TASK_TOTAL_KEY, String.valueOf(maxTasks));
              taskConfig.put(CsvConnector.TASK_HASH_KEY, String.valueOf(index));
              return taskConfig;
            })
        .collect(Collectors.toList());
  }

  @Override
  public List<SettingDef> definitions() {
    return Stream.of(CsvConnector.CSV_SOURCE_DEFINITIONS, super.definitions())
        .flatMap(List::stream)
        .collect(Collectors.toList());
  }
}
