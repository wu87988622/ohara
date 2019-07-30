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

package com.island.ohara.kafka.connector.json;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/** this is a util class helps us to seek default value from definitions */
public final class SettingDefinitions {

  /** the default definitions for all ohara connector. */
  public static final List<SettingDefinition> DEFINITIONS_DEFAULT =
      Arrays.asList(
          SettingDefinition.CONNECTOR_NAME_DEFINITION,
          SettingDefinition.CONNECTOR_KEY_DEFINITION,
          SettingDefinition.CONNECTOR_CLASS_DEFINITION,
          SettingDefinition.COLUMNS_DEFINITION,
          SettingDefinition.KEY_CONVERTER_DEFINITION,
          SettingDefinition.VALUE_CONVERTER_DEFINITION,
          SettingDefinition.WORKER_CLUSTER_NAME_DEFINITION,
          SettingDefinition.NUMBER_OF_TASKS_DEFINITION,
          SettingDefinition.TOPIC_KEYS_DEFINITION,
          SettingDefinition.TOPIC_NAMES_DEFINITION,
          SettingDefinition.TAGS_DEFINITION);

  /**
   * find the default value of version from settings
   *
   * @param settingDefinitions settings
   * @return default value of version. Otherwise, NoSuchElementException will be thrown
   */
  public static String version(List<SettingDefinition> settingDefinitions) {
    return defaultValue(settingDefinitions, SettingDefinition.VERSION_DEFINITION.key());
  }

  /**
   * find the default value of revision from settings
   *
   * @param settingDefinitions settings
   * @return default value of revision. Otherwise, NoSuchElementException will be thrown
   */
  public static String revision(List<SettingDefinition> settingDefinitions) {
    return defaultValue(settingDefinitions, SettingDefinition.REVISION_DEFINITION.key());
  }

  /**
   * find the default value of author from settings
   *
   * @param settingDefinitions settings
   * @return default value of author. Otherwise, NoSuchElementException will be thrown
   */
  public static String author(List<SettingDefinition> settingDefinitions) {
    return defaultValue(settingDefinitions, SettingDefinition.AUTHOR_DEFINITION.key());
  }

  /**
   * find the default value of type name from settings
   *
   * @param settingDefinitions settings
   * @return default value of type name. Otherwise, NoSuchElementException will be thrown
   */
  public static String kind(List<SettingDefinition> settingDefinitions) {
    return defaultValue(settingDefinitions, SettingDefinition.KIND_DEFINITION.key());
  }

  private static String defaultValue(List<SettingDefinition> settingDefinitions, String key) {
    return Optional.ofNullable(
            settingDefinitions.stream()
                .filter(s -> s.key().equals(key))
                .findAny()
                .orElseGet(
                    () -> {
                      throw new NoSuchElementException(
                          key + " doesn't exist! Are you using a stale worker image?");
                    })
                .defaultValue())
        .orElseGet(
            () -> {
              throw new NoSuchElementException(
                  "there is no value matched to " + key + ". Are you using a stale worker image?");
            });
  }

  private SettingDefinitions() {}
}
