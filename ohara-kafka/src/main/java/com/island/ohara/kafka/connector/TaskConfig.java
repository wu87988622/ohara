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

import com.island.ohara.common.annotations.Optional;
import com.island.ohara.common.data.Column;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** this class carries all required configs for row connectors. */
public class TaskConfig {

  public static Builder builder() {
    return new Builder();
  }

  private final String name;
  private final List<String> topics;
  private final List<Column> columns;
  private final Map<String, String> options;

  /**
   * @param name connector name
   * @param topics target topics which row source task should sent data
   * @param columns row columns
   * @param options other configs
   */
  private TaskConfig(
      String name, List<String> topics, List<Column> columns, Map<String, String> options) {
    this.name = name;
    this.topics = topics;
    this.columns = columns;
    this.options = options;
  }

  public String name() {
    return name;
  }

  public List<String> topics() {
    return topics;
  }

  public List<Column> columns() {
    return columns;
  }

  public Map<String, String> options() {
    return options;
  }

  public static class Builder {
    private String name = null;
    private List<String> topics = Collections.emptyList();
    private List<Column> columns = Collections.emptyList();
    private Map<String, String> options = Collections.emptyMap();

    public Builder name(String name) {
      this.name = Objects.requireNonNull(name);
      return this;
    }

    public Builder topic(String topic) {
      return topics(Collections.singletonList(Objects.requireNonNull(topic)));
    }

    public Builder topics(List<String> topics) {
      this.topics = Objects.requireNonNull(topics);
      return this;
    }

    @Optional("default is empty")
    public Builder column(Column column) {
      return columns(Collections.singletonList(Objects.requireNonNull(column)));
    }

    @Optional("default is empty")
    public Builder columns(List<Column> columns) {
      this.columns = Objects.requireNonNull(columns);
      return this;
    }

    @Optional("default is empty")
    public Builder option(String key, String value) {
      return options(Collections.singletonMap(key, value));
    }

    @Optional("default is empty")
    public Builder options(Map<String, String> options) {
      this.options = Objects.requireNonNull(options);
      return this;
    }

    public TaskConfig build() {
      return new TaskConfig(
          Objects.requireNonNull(name),
          Objects.requireNonNull(topics),
          Objects.requireNonNull(columns),
          Objects.requireNonNull(options));
    }
  }
}
