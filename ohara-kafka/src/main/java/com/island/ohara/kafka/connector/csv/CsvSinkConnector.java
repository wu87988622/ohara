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

import static com.island.ohara.kafka.connector.csv.CsvConnectorDefinitions.FILE_ENCODE_DEFINITION;
import static com.island.ohara.kafka.connector.csv.CsvConnectorDefinitions.FILE_NEED_HEADER_DEFINITION;
import static com.island.ohara.kafka.connector.csv.CsvConnectorDefinitions.FLUSH_SIZE_DEFINITION;
import static com.island.ohara.kafka.connector.csv.CsvConnectorDefinitions.OUTPUT_FOLDER_DEFINITION;
import static com.island.ohara.kafka.connector.csv.CsvConnectorDefinitions.ROTATE_INTERVAL_MS_DEFINITION;

import com.island.ohara.common.setting.SettingDef;
import com.island.ohara.kafka.connector.RowSinkConnector;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A wrap to RowSinkConnector. The difference between CsvSinkConnector and RowSinkConnector is that
 * CsvSinkConnector already has some default definitions, as follows:
 *
 * <ul>
 *   <li>TOPICS_DIR_DEFINITION: Read csv data from topic and then write to this folder
 *   <li>FLUSH_SIZE_DEFINITION: Number of records write to store before invoking file commits
 *   <li>ROTATE_INTERVAL_MS_DEFINITION: Commit file time
 *   <li>FILE_NEED_HEADER_DEFINITION: File need header for flush data
 *   <li>FILE_ENCODE_DEFINITION: File encode for write to file
 * </ul>
 */
public abstract class CsvSinkConnector extends RowSinkConnector {

  /** @return custom setting definitions from sub csv connectors */
  protected Map<String, SettingDef> customCsvSettingDefinitions() {
    return Collections.emptyMap();
  }

  @Override
  protected final Map<String, SettingDef> customSettingDefinitions() {
    Map<String, SettingDef> finalDefinitions = new TreeMap<>(customCsvSettingDefinitions());
    finalDefinitions.putAll(
        Stream.of(
                OUTPUT_FOLDER_DEFINITION,
                FLUSH_SIZE_DEFINITION,
                ROTATE_INTERVAL_MS_DEFINITION,
                FILE_NEED_HEADER_DEFINITION,
                FILE_ENCODE_DEFINITION)
            .collect(Collectors.toMap(SettingDef::key, Function.identity())));
    return Collections.unmodifiableMap(finalDefinitions);
  }
}
