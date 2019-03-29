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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.island.ohara.common.util.CommonUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.kafka.connect.runtime.rest.entities.ConfigInfos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SettingInfo implements JsonObject {
  public static final Logger LOG = LoggerFactory.getLogger(SettingInfo.class);
  private static final String ERROR_COUNT_KEY = "errorCount";
  private static final String SETTINGS_KEY = "settings";

  public static SettingInfo of(ConfigInfos configInfos) {
    List<Setting> settings =
        configInfos.values().stream()
            .map(
                configInfo -> {
                  try {
                    return Optional.of(Setting.of(configInfo));
                  } catch (IllegalArgumentException e) {
                    // if configInfo is not serialized by ohara, we will get JsonParseException in
                    // parsing json.
                    if (e.getCause() instanceof JsonParseException) {
                      LOG.debug(
                          "fails to serializer "
                              + configInfo
                              + " from "
                              + configInfos.name()
                              + ". [TODO] we should replace it by ohara definition",
                          e);
                      return Optional.empty();
                    }
                    throw e;
                  }
                })
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(s -> (Setting) s)
            .collect(Collectors.toList());
    if (settings.isEmpty())
      throw new IllegalArgumentException(
          "failed to parse ohara data stored in kafka. Are you using stale worker image?");
    return of(settings);
  }

  public static SettingInfo ofJson(String json) {
    return JsonUtils.toObject(json, new TypeReference<SettingInfo>() {});
  }

  public static SettingInfo of(List<Setting> settings) {
    return new SettingInfo(
        (int) settings.stream().map(Setting::value).filter(v -> !v.errors().isEmpty()).count(),
        settings);
  }

  private final int errorCount;
  private final List<Setting> settings;

  @JsonCreator
  private SettingInfo(
      @JsonProperty(ERROR_COUNT_KEY) int errorCount,
      @JsonProperty(SETTINGS_KEY) List<Setting> settings) {
    this.errorCount = CommonUtils.requirePositiveInt(errorCount);
    this.settings = new ArrayList<>(CommonUtils.requireNonEmpty(settings));
  }

  // ------------------------[helper method]------------------------//
  public Optional<String> value(String key) {
    List<String> values =
        settings.stream()
            .map(Setting::value)
            .filter(v -> v.value() != null && v.key().equals(key))
            .map(SettingValue::value)
            .collect(Collectors.toList());
    if (values.isEmpty()) return Optional.empty();
    else return Optional.of(values.get(0));
  }

  // ------------------------[common]------------------------//
  public Optional<String> className() {
    return value(SettingDefinition.CONNECTOR_CLASS_DEFINITION.key());
  }

  public List<String> topicNames() {
    return value(SettingDefinition.TOPIC_NAMES_DEFINITION.key())
        .map(StringList::ofKafkaList)
        .orElse(Collections.emptyList());
  }

  public Optional<Integer> numberOfTasks() {
    return value(SettingDefinition.NUMBER_OF_TASKS_DEFINITION.key()).map(Integer::valueOf);
  }

  public Optional<String> author() {
    return value(SettingDefinition.AUTHOR_DEFINITION.key());
  }

  public Optional<String> version() {
    return value(SettingDefinition.VERSION_DEFINITION.key());
  }

  public Optional<String> revision() {
    return value(SettingDefinition.REVISION_DEFINITION.key());
  }

  public Optional<String> workerClusterName() {
    return value(SettingDefinition.WORKER_CLUSTER_NAME_DEFINITION.key());
  }

  public Optional<String> connectorType() {
    return value(SettingDefinition.KIND_DEFINITION.key());
  }

  // ------------------------[json]------------------------//
  @JsonProperty(ERROR_COUNT_KEY)
  public int errorCount() {
    return errorCount;
  }

  @JsonProperty(SETTINGS_KEY)
  public List<Setting> settings() {
    return Collections.unmodifiableList(settings);
  }

  // ------------------------[object]------------------------//
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof SettingInfo)
      return toJsonString().equals(((SettingInfo) obj).toJsonString());
    return false;
  }

  @Override
  public int hashCode() {
    return toJsonString().hashCode();
  }

  @Override
  public String toString() {
    return toJsonString();
  }
}
