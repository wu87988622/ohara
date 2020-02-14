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

import static com.island.ohara.kafka.connector.csv.CsvConnectorDefinitions.COMPLETED_FOLDER_DEFINITION;
import static com.island.ohara.kafka.connector.csv.CsvConnectorDefinitions.ERROR_FOLDER_DEFINITION;
import static com.island.ohara.kafka.connector.csv.CsvConnectorDefinitions.FILE_ENCODE_DEFINITION;
import static com.island.ohara.kafka.connector.csv.CsvConnectorDefinitions.INPUT_FOLDER_DEFINITION;

import com.island.ohara.common.setting.SettingDef;
import com.island.ohara.kafka.connector.RowSourceConnector;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
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
              taskConfig.put(CsvConnectorDefinitions.TASK_TOTAL_KEY, String.valueOf(maxTasks));
              taskConfig.put(CsvConnectorDefinitions.TASK_HASH_KEY, String.valueOf(index));
              return taskConfig;
            })
        .collect(Collectors.toList());
  }

  /** @return custom setting definitions from sub csv connectors */
  protected Map<String, SettingDef> customCsvSettingDefinitions() {
    return Collections.emptyMap();
  }

  @Override
  protected final Map<String, SettingDef> customSettingDefinitions() {
    Map<String, SettingDef> finalDefinitions = new TreeMap<>(customCsvSettingDefinitions());
    finalDefinitions.putAll(
        Stream.of(
                INPUT_FOLDER_DEFINITION,
                COMPLETED_FOLDER_DEFINITION,
                ERROR_FOLDER_DEFINITION,
                FILE_ENCODE_DEFINITION)
            .collect(Collectors.toMap(SettingDef::key, Function.identity())));
    return Collections.unmodifiableMap(finalDefinitions);
  }
}
