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

import com.island.ohara.common.setting.SettingDef;
import com.island.ohara.common.setting.TopicKey;
import com.island.ohara.common.util.CommonUtils;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The entry class for define streamApp definitions
 *
 * <p>The data we keep in this class is use the format : List&lt;SettingDef&gt;
 */
public final class StreamDefinitions {

  private final List<SettingDef> configDefs;

  StreamDefinitions(List<SettingDef> configDefs) {
    this.configDefs = Collections.unmodifiableList(configDefs);
  }

  /**
   * create default configurations
   *
   * @return StreamDefinitions object
   */
  public static StreamDefinitions create() {
    return new StreamDefinitions(StreamDefUtils.DEFAULT);
  }

  /**
   * Add a extra {@code SettingDef} into this stream definitions.
   *
   * @param settingDef config object
   * @return StreamDefinitions
   */
  public static StreamDefinitions with(SettingDef settingDef) {
    // check the duplicate definitions
    if (StreamDefUtils.DEFAULT.stream().anyMatch(c -> c.key().equals(settingDef.key()))) {
      throw new IllegalArgumentException(
          String.format("StreamDefinitions: %s is defined twice", settingDef.key()));
    }

    return new StreamDefinitions(
        Stream.of(StreamDefUtils.DEFAULT, Collections.singletonList(settingDef))
            .flatMap(List::stream)
            .collect(Collectors.toList()));
  }

  /**
   * Add a extra {@code SettingDef} list into this stream definitions.
   *
   * @param settingDefList config list
   * @return StreamDefinitions
   */
  public static StreamDefinitions withAll(List<SettingDef> settingDefList) {
    // check the duplicate definitions
    List<String> newKeys =
        settingDefList.stream().map(SettingDef::key).collect(Collectors.toList());
    List<String> oriKeys =
        StreamDefUtils.DEFAULT.stream().map(SettingDef::key).collect(Collectors.toList());
    if (StreamDefUtils.DEFAULT.stream().map(SettingDef::key).anyMatch(newKeys::contains)) {
      throw new IllegalArgumentException(
          String.format(
              "Some config of list: [%s] are defined twice. Original: [%s]",
              String.join(",", newKeys), String.join(",", oriKeys)));
    }

    return new StreamDefinitions(
        Stream.of(StreamDefUtils.DEFAULT, settingDefList)
            .flatMap(List::stream)
            .collect(Collectors.toList()));
  }

  /**
   * Get all {@code Config.name} from this class.
   *
   * @return config name list
   */
  public List<String> keys() {
    return configDefs.stream().map(SettingDef::key).collect(Collectors.toList());
  }

  /**
   * Get all {@code SettingDef} from this class.
   *
   * @return config object list
   */
  public List<SettingDef> settingDefinitions() {
    return configDefs;
  }

  /**
   * Get value from specific name. Note: This is a helper method for container environment.
   *
   * @param key config key
   * @return value from container environment, {@code Optional.empty()} if absent
   */
  public Optional<String> string(String key) {
    return Optional.ofNullable(System.getenv(key)).map(CommonUtils::fromEnvString);
  }

  /** @return the name of this streamApp */
  public String name() {
    return string(StreamDefUtils.NAME_DEFINITION.key())
        .orElseThrow(() -> new RuntimeException("NAME_DEFINITION not found in env."));
  }

  /** @return brokers' connection props */
  public String brokerConnectionProps() {
    return string(StreamDefUtils.BROKER_DEFINITION.key())
        .orElseThrow(() -> new RuntimeException("BROKER_DEFINITION not found in env."));
  }

  /** @return the keys of from topics */
  public List<TopicKey> fromTopicKeys() {
    return TopicKey.toTopicKeys(
        string(StreamDefUtils.FROM_TOPIC_KEYS_DEFINITION.key())
            .orElseThrow(
                () -> new RuntimeException("FROM_TOPIC_KEYS_DEFINITION not found in env.")));
  }

  /** @return the keys of to topics */
  public List<TopicKey> toTopicKeys() {
    return TopicKey.toTopicKeys(
        string(StreamDefUtils.TO_TOPIC_KEYS_DEFINITION.key())
            .orElseThrow(() -> new RuntimeException("TO_TOPIC_KEYS_DEFINITION not found in env.")));
  }
}
