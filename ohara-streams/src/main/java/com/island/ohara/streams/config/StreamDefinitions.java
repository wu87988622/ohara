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

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.island.ohara.common.json.JsonObject;
import com.island.ohara.common.json.JsonUtils;
import com.island.ohara.common.setting.SettingDef;
import com.island.ohara.common.setting.SettingDef.Type;
import com.island.ohara.common.setting.TopicKey;
import com.island.ohara.common.util.CommonUtils;
import com.island.ohara.common.util.VersionUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
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

  /**
   * Since the fastxml generate the json object with {"name": "value"} format, We need to handle the
   * "unused" name in parsing json object for example, { "configs": [ "some.key": "some.value", ...
   * ] } and the "configs" keyword is useless when we generate the definitions.
   */
  static final String CONFIGS_FIELD_NAME = "configs";

  static final String CORE_GROUP = "core";

  /** This is the default configurations we will load into {@code StreamDefinitions}. */
  private static final AtomicInteger ORDER_COUNTER = new AtomicInteger(0);

  public static final SettingDef BROKER_CLUSTER_NAME_DEFINITION =
      SettingDef.builder()
          .key("broker.cluster.name")
          .group(CORE_GROUP)
          .orderInGroup(ORDER_COUNTER.getAndIncrement())
          .displayName("Broker cluster name")
          .documentation("the name of broker cluster used to transfer data for this streamapp")
          .valueType(Type.STRING)
          .internal()
          .build();

  public static final SettingDef BROKER_DEFINITION =
      SettingDef.builder()
          .key("servers")
          .group(CORE_GROUP)
          .orderInGroup(ORDER_COUNTER.getAndIncrement())
          .displayName("Broker list")
          .documentation("The broker list of current workspace")
          .valueType(Type.STRING)
          .internal()
          .build();

  public static final SettingDef IMAGE_NAME_DEFINITION =
      SettingDef.builder()
          .key("imageName")
          .group(CORE_GROUP)
          .orderInGroup(ORDER_COUNTER.getAndIncrement())
          .displayName("Image name")
          .documentation("The image name of this streamApp running with")
          .valueType(Type.STRING)
          .optional()
          // In manager, user cannot change the image name
          .readonly()
          .build();

  public static final SettingDef NAME_DEFINITION =
      SettingDef.builder()
          .key("name")
          .group(CORE_GROUP)
          .orderInGroup(ORDER_COUNTER.getAndIncrement())
          .displayName("StreamApp name")
          .documentation("The unique name of this streamApp")
          .valueType(Type.STRING)
          .optional()
          .build();

  public static final SettingDef GROUP_DEFINITION =
      SettingDef.builder()
          .key("group")
          .group(CORE_GROUP)
          .orderInGroup(ORDER_COUNTER.getAndIncrement())
          .displayName("StreamApp group")
          .internal()
          .documentation("The group name of this streamApp")
          .valueType(Type.STRING)
          .build();

  public static final SettingDef JAR_KEY_DEFINITION =
      SettingDef.builder()
          .key("jarKey")
          .group(CORE_GROUP)
          .orderInGroup(ORDER_COUNTER.getAndIncrement())
          .displayName("Jar primary key")
          .documentation("The jar key of this streamApp using")
          .valueType(Type.JAR_KEY)
          .build();

  /** this field is used to store whole info for a jar file */
  public static final SettingDef JAR_INFO_DEFINITION =
      SettingDef.builder()
          .key("jarInfo")
          .group(CORE_GROUP)
          .orderInGroup(ORDER_COUNTER.getAndIncrement())
          .displayName("Jar Info")
          .documentation("The jar info of this streamApp using")
          .valueType(Type.STRING)
          .internal()
          .build();

  public static final SettingDef FROM_TOPIC_KEYS_DEFINITION =
      SettingDef.builder()
          .key("from")
          .group(CORE_GROUP)
          .orderInGroup(ORDER_COUNTER.getAndIncrement())
          .reference(SettingDef.Reference.TOPIC)
          .displayName("From topic of data consuming from")
          .documentation("The topic name of this streamApp should consume from")
          .valueType(Type.TOPIC_KEYS)
          .build();

  public static final SettingDef TO_TOPIC_KEYS_DEFINITION =
      SettingDef.builder()
          .key("to")
          .group(CORE_GROUP)
          .orderInGroup(ORDER_COUNTER.getAndIncrement())
          .reference(SettingDef.Reference.TOPIC)
          .displayName("To topic of data produce to")
          .documentation("The topic name of this streamApp should produce to")
          .valueType(Type.TOPIC_KEYS)
          .build();

  public static final SettingDef JMX_PORT_DEFINITION =
      SettingDef.builder()
          .key("jmxPort")
          .group(CORE_GROUP)
          .orderInGroup(ORDER_COUNTER.getAndIncrement())
          .displayName("JMX export port")
          .documentation("The port of this streamApp using to export jmx metrics")
          .valueType(Type.PORT)
          .optional()
          .build();

  public static final SettingDef INSTANCES_DEFINITION =
      SettingDef.builder()
          .key("instances")
          .group(CORE_GROUP)
          .orderInGroup(ORDER_COUNTER.getAndIncrement())
          .displayName("Instances")
          .documentation("The running container number of this streamApp")
          .valueType(Type.INT)
          .build();

  public static final SettingDef NODE_NAMES_DEFINITION =
      SettingDef.builder()
          .key("nodeNames")
          .group(CORE_GROUP)
          .orderInGroup(ORDER_COUNTER.getAndIncrement())
          .displayName("Node name list")
          .documentation("The used node name list of this streamApp")
          .valueType(Type.ARRAY)
          .build();

  public static final SettingDef EXACTLY_ONCE_DEFINITION =
      SettingDef.builder()
          .key("exactlyOnce")
          .group(CORE_GROUP)
          .orderInGroup(ORDER_COUNTER.getAndIncrement())
          .displayName("Enable exactly once")
          .documentation("Enable this streamApp to process each record exactly once")
          .readonly()
          .valueType(Type.BOOLEAN)
          .optional("false")
          .build();

  public static final SettingDef VERSION_DEFINITION =
      SettingDef.builder()
          .key("version")
          .group(CORE_GROUP)
          .orderInGroup(ORDER_COUNTER.getAndIncrement())
          .displayName("Version")
          .documentation("Version of streamApp")
          .readonly()
          .valueType(Type.STRING)
          .optional(VersionUtils.VERSION)
          .build();

  public static final SettingDef REVISION_DEFINITION =
      SettingDef.builder()
          .key("revision")
          .group(CORE_GROUP)
          .orderInGroup(ORDER_COUNTER.getAndIncrement())
          .displayName("Revision")
          .readonly()
          .documentation("Revision of streamApp")
          .valueType(Type.STRING)
          .optional(VersionUtils.REVISION)
          .build();

  public static final SettingDef AUTHOR_DEFINITION =
      SettingDef.builder()
          .key("author")
          .group(CORE_GROUP)
          .orderInGroup(ORDER_COUNTER.getAndIncrement())
          .displayName("Author")
          .readonly()
          .documentation("Author of streamApp")
          .valueType(Type.STRING)
          .optional(VersionUtils.USER)
          .build();

  public static final SettingDef TAGS_DEFINITION =
      SettingDef.builder()
          .key("tags")
          .group(CORE_GROUP)
          .orderInGroup(ORDER_COUNTER.getAndIncrement())
          .displayName("Tags")
          .documentation("Tags of streamApp")
          .valueType(Type.TAGS)
          // In manager, the tags field is for internal use
          .internal()
          .optional()
          .build();

  // this is the jar url definition that used in container start argument
  public static final SettingDef JAR_URL_DEFINITION =
      SettingDef.builder()
          .key("jarUrl")
          .group(CORE_GROUP)
          .orderInGroup(ORDER_COUNTER.getAndIncrement())
          .displayName("Jar url key")
          .documentation("The jar url that will be used in streamApp")
          .internal()
          .valueType(Type.STRING)
          .build();

  /**
   * create default configurations
   *
   * @return StreamDefinitions object
   */
  public static StreamDefinitions create() {
    return new StreamDefinitions(DEFAULT);
  }

  /**
   * Load configs from default definitions.
   *
   * <p>This field is associated to a immutable map.
   */
  public static final Map<String, SettingDef> DEFAULT =
      Stream.of(
              StreamDefinitions.BROKER_DEFINITION,
              StreamDefinitions.IMAGE_NAME_DEFINITION,
              StreamDefinitions.NAME_DEFINITION,
              StreamDefinitions.GROUP_DEFINITION,
              StreamDefinitions.FROM_TOPIC_KEYS_DEFINITION,
              StreamDefinitions.TO_TOPIC_KEYS_DEFINITION,
              StreamDefinitions.JMX_PORT_DEFINITION,
              StreamDefinitions.INSTANCES_DEFINITION,
              StreamDefinitions.NODE_NAMES_DEFINITION,
              StreamDefinitions.VERSION_DEFINITION,
              StreamDefinitions.REVISION_DEFINITION,
              StreamDefinitions.AUTHOR_DEFINITION,
              StreamDefinitions.TAGS_DEFINITION,
              StreamDefinitions.JAR_KEY_DEFINITION,
              StreamDefinitions.JAR_INFO_DEFINITION)
          .collect(Collectors.toMap(SettingDef::key, Function.identity()));

  private Map<String, SettingDef> configs;

  private StreamDefinitions() {}

  private StreamDefinitions(Map<String, SettingDef> configs) {
    // sort the configs
    this.configs = new TreeMap<>(configs);
  }

  /**
   * Add a {@code Config} into this class.
   *
   * @param config config object
   * @return this StreamDefinitions
   */
  public StreamDefinitions add(SettingDef config) {
    if (configs.containsKey(config.key())) {
      throw new IllegalArgumentException(
          String.format("StreamDefinitions: %s is defined twice", config.key()));
    }
    configs.put(config.key(), config);
    return this;
  }

  /**
   * Add {@code Config} list into this class.
   *
   * @param configList config list
   * @return this StreamDefinitions
   */
  public StreamDefinitions addAll(List<SettingDef> configList) {
    List<String> keys = configList.stream().map(SettingDef::key).collect(Collectors.toList());
    if (configs.keySet().stream().anyMatch(keys::contains)) {
      throw new IllegalArgumentException(
          String.format(
              "Some config of list: [%s] are defined twice. Original: [%s]",
              String.join(",", keys), String.join(",", configs.keySet())));
    }
    configs.putAll(
        configList.stream().collect(Collectors.toMap(SettingDef::key, Function.identity())));
    return this;
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
   * @param key config key
   * @return value from container environment
   */
  public String string(String key) {
    return CommonUtils.fromEnvString(Objects.requireNonNull(System.getenv(key)));
  }

  public Optional<String> stringOption(String key) {
    return Optional.ofNullable(System.getenv(key)).map(CommonUtils::fromEnvString);
  }

  /**
   * this is a workaround function since the LaunchImpl does not care null point. Hence, we can't do
   * totally check for env variables for it ...
   *
   * @return string or none
   */
  public Optional<String> nameOption() {
    return stringOption(StreamDefinitions.NAME_DEFINITION.key());
  }

  /** @return the name of this streamapp */
  public String name() {
    return string(StreamDefinitions.NAME_DEFINITION.key());
  }

  /**
   * this is a workaround function since the LaunchImpl does not care null point. Hence, we can't do
   * totally check for env variables for it ...
   *
   * @return string or none
   */
  public Optional<String> brokerConnectionPropsOption() {
    return stringOption(StreamDefinitions.BROKER_DEFINITION.key());
  }

  /** @return brokers' connection props */
  public String brokerConnectionProps() {
    return string(StreamDefinitions.BROKER_DEFINITION.key());
  }

  /** @return the keys of from topics */
  public List<TopicKey> fromTopicKeys() {
    return stringOption(StreamDefinitions.FROM_TOPIC_KEYS_DEFINITION.key())
        .map(TopicKey::toTopicKeys)
        .orElse(Collections.emptyList());
  }

  /** @return the keys of to topics */
  public List<TopicKey> toTopicKeys() {
    return stringOption(StreamDefinitions.TO_TOPIC_KEYS_DEFINITION.key())
        .map(TopicKey::toTopicKeys)
        .orElse(Collections.emptyList());
  }

  /**
   * Get all {@code SettingDef} from this class.
   *
   * @return config object list
   */
  @JsonGetter(CONFIGS_FIELD_NAME)
  public List<SettingDef> values() {
    return new ArrayList<>(configs.values());
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
            .get(CONFIGS_FIELD_NAME).stream()
            .collect(Collectors.toMap(SettingDef::key, Function.identity())));
  }
}
