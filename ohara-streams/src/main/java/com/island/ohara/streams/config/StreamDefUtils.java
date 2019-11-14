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
import com.island.ohara.common.setting.SettingDef.Type;
import com.island.ohara.common.util.VersionUtils;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * This is an helper class for getting / setting {@link com.island.ohara.common.setting.SettingDef}
 * for StreamApp.
 */
public final class StreamDefUtils {

  private static final String CORE_GROUP = "core";

  /** This is the default configurations we will load into {@code StreamDefUtils}. */
  private static final AtomicInteger ORDER_COUNTER = new AtomicInteger(0);

  public static final SettingDef BROKER_CLUSTER_KEY_DEFINITION =
      SettingDef.builder()
          .key("brokerClusterKey")
          .group(CORE_GROUP)
          .orderInGroup(ORDER_COUNTER.getAndIncrement())
          .displayName("Broker cluster key")
          .documentation("the key of broker cluster used to transfer data for this streamApp")
          .required(Type.OBJECT_KEY)
          .reference(SettingDef.Reference.BROKER_CLUSTER)
          .build();

  public static final SettingDef BROKER_DEFINITION =
      SettingDef.builder()
          .key("servers")
          .group(CORE_GROUP)
          .orderInGroup(ORDER_COUNTER.getAndIncrement())
          .displayName("Broker list")
          .documentation("The broker list of current workspace")
          .required(Type.STRING)
          .internal()
          .build();

  public static final SettingDef IMAGE_NAME_DEFINITION =
      SettingDef.builder()
          .key("imageName")
          .group(CORE_GROUP)
          .orderInGroup(ORDER_COUNTER.getAndIncrement())
          .displayName("Image name")
          .documentation("The image name of this streamApp running with")
          .optional("oharastream/streamapp:" + VersionUtils.VERSION)
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
          .stringWithRandomDefault()
          .build();

  public static final SettingDef GROUP_DEFINITION =
      SettingDef.builder()
          .key("group")
          .group(CORE_GROUP)
          .orderInGroup(ORDER_COUNTER.getAndIncrement())
          .displayName("StreamApp group")
          .documentation("The unique group of this streamApp")
          .optional("default")
          .build();

  public static final SettingDef JAR_KEY_DEFINITION =
      SettingDef.builder()
          .key("jarKey")
          .group(CORE_GROUP)
          .orderInGroup(ORDER_COUNTER.getAndIncrement())
          .displayName("Jar primary key")
          .documentation("The jar key of this streamApp using")
          .required(Type.OBJECT_KEY)
          .reference(SettingDef.Reference.FILE)
          .build();

  public static final SettingDef CLASS_NAME_DEFINITION =
      SettingDef.builder()
          .key("className")
          .group(CORE_GROUP)
          .orderInGroup(ORDER_COUNTER.getAndIncrement())
          .displayName("the stream class you want to run")
          .documentation(
              "the stream class running in all stream nodes. If you don't define it, Configurator will seek all jar files to find the available one.")
          .optional(Type.CLASS)
          .build();

  public static final SettingDef FROM_TOPIC_KEYS_DEFINITION =
      SettingDef.builder()
          .key("from")
          .group(CORE_GROUP)
          .orderInGroup(ORDER_COUNTER.getAndIncrement())
          .reference(SettingDef.Reference.TOPIC)
          .displayName("From topic of data consuming from")
          .documentation("The topic name of this streamApp should consume from")
          // we have to make this field optional since our UI needs to create stream without
          // topics...
          .optional(Type.OBJECT_KEYS)
          .build();

  public static final SettingDef TO_TOPIC_KEYS_DEFINITION =
      SettingDef.builder()
          .key("to")
          .group(CORE_GROUP)
          .orderInGroup(ORDER_COUNTER.getAndIncrement())
          .reference(SettingDef.Reference.TOPIC)
          .displayName("To topic of data produce to")
          .documentation("The topic name of this streamApp should produce to")
          // we have to make this field optional since our UI needs to create stream without
          // topics...
          .optional(Type.OBJECT_KEYS)
          .build();

  public static final SettingDef JMX_PORT_DEFINITION =
      SettingDef.builder()
          .key("jmxPort")
          .group(CORE_GROUP)
          .orderInGroup(ORDER_COUNTER.getAndIncrement())
          .displayName("JMX export port")
          .documentation("The port of this streamApp using to export jmx metrics")
          .bindingPortWithRandomDefault()
          .build();

  public static final SettingDef NODE_NAMES_DEFINITION =
      SettingDef.builder()
          .key("nodeNames")
          .group(CORE_GROUP)
          .orderInGroup(ORDER_COUNTER.getAndIncrement())
          .displayName("Node name list")
          .documentation("The used node name list of this streamApp")
          .blacklist(new HashSet<>(Arrays.asList("stop", "start", "pause", "resume")))
          .build();

  public static final String STREAM_APPLICATION = "streamApp";
  /**
   * annotate the kind of this streamapp. This value is immutable and it is useful in parsing
   * definitions dynamically. The value of kind help us to understand the "master" of those
   * definitions.
   */
  public static final SettingDef KIND_DEFINITION =
      SettingDef.builder()
          .displayName("kind")
          // this key must be equal to ConnectorDefUtils.KIND_KEY
          .key("kind")
          .documentation("kind of stream application")
          .optional(STREAM_APPLICATION)
          .readonly()
          .build();

  public static final SettingDef VERSION_DEFINITION =
      SettingDef.builder()
          .key("version")
          .group(CORE_GROUP)
          .orderInGroup(ORDER_COUNTER.getAndIncrement())
          .displayName("Version")
          .documentation("Version of streamApp")
          .readonly()
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
          .optional(VersionUtils.USER)
          .build();

  public static final SettingDef TAGS_DEFINITION =
      SettingDef.builder()
          .key("tags")
          .group(CORE_GROUP)
          .orderInGroup(ORDER_COUNTER.getAndIncrement())
          .displayName("Tags")
          .documentation("Tags of streamApp")
          .optional(Type.TAGS)
          .build();

  // this is the streamApp metric group definition
  public static final String STREAM_METRICS_GROUP_DEFAULT = "streamapp";

  /**
   * Load configDefs from default definitions.
   *
   * <p>This field is associated to a immutable map.
   */
  public static final List<SettingDef> DEFAULT =
      Arrays.stream(StreamDefUtils.class.getDeclaredFields())
          .filter(field -> field.getType().isAssignableFrom(SettingDef.class))
          .map(
              field -> {
                try {
                  return (SettingDef) field.get(new StreamDefUtils());
                } catch (IllegalAccessException e) {
                  throw new IllegalArgumentException("field is not able cast to SettingDef", e);
                }
              })
          .collect(Collectors.toList());

  // disable constructor
  private StreamDefUtils() {}
}
