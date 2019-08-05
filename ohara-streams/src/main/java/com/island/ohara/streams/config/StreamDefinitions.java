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

package com.island.ohara.streams.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.island.ohara.common.json.JsonObject;
import com.island.ohara.common.json.JsonUtils;
import com.island.ohara.common.setting.SettingDef;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This is an entry class for getting / setting {@link com.island.ohara.common.setting.SettingDef}
 * of StreamApp.
 *
 * <p>The data we keep in this class is use the format : Map(SettingDef.key, SettingDef)
 */
public final class StreamDefinitions implements JsonObject {

  static final String CORE_GROUP = "core";

  private final Map<String, SettingDef> configs;

  @JsonCreator
  private StreamDefinitions(Map<String, SettingDef> configs) {
    // sort the configs
    this.configs = new TreeMap<>(configs);
  }

  /** get default configurations */
  public static final StreamDefinitions DEFAULT = new StreamDefinitions(getDefault());

  /**
   * Load configs from default definitions. Note that we will return <b>Map.Empty</b> if the class
   * not met the requirement.
   *
   * @return map of configs with format : <b>(SettingDef.key, SettingDef)</b>
   * @see DefaultConfigs
   */
  private static Map<String, SettingDef> getDefault() {
    return Stream.of(
            DefaultConfigs.APPID_DEFINITION,
            DefaultConfigs.BROKER_DEFINITION,
            DefaultConfigs.FROM_TOPIC_DEFINITION,
            DefaultConfigs.TO_TOPIC_DEFINITION,
            DefaultConfigs.EXACTLY_ONCE_DEFINITION)
        .collect(Collectors.toMap(SettingDef::key, Function.identity()));
  }

  /**
   * Add a {@code Config} into this class.
   *
   * @param config config object
   * @return this StreamDefinitions
   */
  public static StreamDefinitions add(SettingDef config) {
    Map<String, SettingDef> configs = StreamDefinitions.getDefault();
    if (configs.containsKey(config.key())) {
      throw new IllegalArgumentException(
          String.format("StreamDefinitions: %s is defined twice", config.key()));
    }
    configs.put(config.key(), config);
    return new StreamDefinitions(configs);
  }

  /**
   * Add {@code Config} list into this class.
   *
   * @param configMap config map list
   * @return this StreamDefinitions
   */
  public static StreamDefinitions addAll(Map<String, SettingDef> configMap) {
    Map<String, SettingDef> configs = StreamDefinitions.getDefault();
    if (configs.keySet().stream().anyMatch(p -> configMap.keySet().contains(p))) {
      throw new IllegalArgumentException(
          String.format(
              "Some config of list: [%s] are defined twice. Original: [%s]",
              String.join(",", configMap.keySet()), String.join(",", configs.keySet())));
    }
    configs.putAll(configMap);
    return new StreamDefinitions(configs);
  }

  /**
   * Get all {@code Config.name} from this class.
   *
   * @return config name list
   */
  public List<String> keys() {
    return new ArrayList<>(configs.keySet());
  }

  /**
   * Get value from specific name. Note: This is a helper method for container environment.
   *
   * @param name config name
   * @return value from container environment
   */
  public String get(String name) {
    return System.getenv(name);
  }

  /**
   * Get all {@code SettingDef} from this class.
   *
   * @return config object list
   */
  @JsonGetter
  List<SettingDef> values() {
    return new ArrayList<>(configs.values());
  }

  /**
   * This is the default configurations we will load into {@code StreamDefinitions}.
   *
   * <p>Do not change this class except you know what you are doing.
   *
   * <p>default fields contains:
   *
   * <ul>
   *   <li>servers
   *   <li>appId
   *   <li>from
   *   <li>to
   *   <li>exactlyOnce
   * </ul>
   */
  private static class DefaultConfigs {

    private static SettingDef BROKER_DEFINITION =
        SettingDef.builder()
            .key("streamapp.servers")
            .group(CORE_GROUP)
            .displayName("Broker List")
            .readonly()
            .documentation("The broker list of current workspace")
            .build();

    private static SettingDef APPID_DEFINITION =
        SettingDef.builder()
            .key("streamapp.appId")
            .group(CORE_GROUP)
            .displayName("Application ID")
            .documentation("The unique name of this streamApp")
            .build();

    private static SettingDef FROM_TOPIC_DEFINITION =
        SettingDef.builder()
            .key("streamapp.from")
            .group(CORE_GROUP)
            .displayName("Topic of Consuming from")
            .documentation("The topic name of this streamApp should consume from")
            .build();

    private static SettingDef TO_TOPIC_DEFINITION =
        SettingDef.builder()
            .key("streamapp.to")
            .group(CORE_GROUP)
            .displayName("Topic of Producing to")
            .documentation("The topic name of this streamApp should produce to")
            .build();

    private static SettingDef EXACTLY_ONCE_DEFINITION =
        SettingDef.builder()
            .key("streamapp.exactlyOnce")
            .group(CORE_GROUP)
            .displayName("Enable Exactly Once")
            .documentation("Enable this streamApp to process each record exactly once")
            .build();
  }

  /**
   * Compare two objects are equal or not. We compare JSON string since different order of settings
   * will be sorted and should be equal.
   *
   * @param obj other config
   * @return true if equals object
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof StreamDefinitions) {
      return toString().equals(obj.toString());
    } else return false;
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  @Override
  public String toString() {
    return toJsonString();
  }

  public static StreamDefinitions ofJson(String json) {
    return new StreamDefinitions(
        JsonUtils.toObject(json, new TypeReference<Map<String, List<SettingDef>>>() {})
            .get("values").stream()
            .collect(Collectors.toMap(SettingDef::key, Function.identity())));
  }
}
