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

package com.island.ohara.kafka.connector;

import com.island.ohara.kafka.connector.json.SettingDefinition;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.kafka.common.config.ConfigDef;

final class ConnectorUtils {
  private static SettingDefinition copy(String value, SettingDefinition definition) {
    return SettingDefinition.builder(definition).optional(value).build();
  }

  static List<SettingDefinition> toSettingDefinitions(
      List<SettingDefinition> settingDefinitions, String version, String revision, String author) {
    return Stream.of(
            settingDefinitions,
            Arrays.asList(
                copy(version, SettingDefinition.VERSION_DEFINITION),
                copy(revision, SettingDefinition.REVISION_DEFINITION),
                copy(author, SettingDefinition.AUTHOR_DEFINITION)))
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  static ConfigDef toConfigDef(List<SettingDefinition> settingDefinitions) {
    ConfigDef def = new ConfigDef();
    settingDefinitions.stream().map(SettingDefinition::toConfigKey).forEach(def::define);
    return def;
  }

  private ConnectorUtils() {}
}
