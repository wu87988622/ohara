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
  private final List<Column> schema;
  private final Map<String, String> options;

  /**
   * @param name connector name
   * @param topics target topics which row source task should sent data
   * @param schema row schema
   * @param options other configs
   */
  private TaskConfig(
      String name, List<String> topics, List<Column> schema, Map<String, String> options) {
    this.name = name;
    this.topics = topics;
    this.schema = schema;
    this.options = options;
  }

  public String name() {
    return name;
  }

  public List<String> topics() {
    return topics;
  }

  public List<Column> schema() {
    return schema;
  }

  public Map<String, String> options() {
    return options;
  }

  public static class Builder {
    private String name = null;
    private List<String> topics = Collections.emptyList();
    private List<Column> schema = Collections.emptyList();
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
    public Builder schema(Column column) {
      return schema(Collections.singletonList(Objects.requireNonNull(column)));
    }

    @Optional("default is empty")
    public Builder schema(List<Column> schema) {
      this.schema = Objects.requireNonNull(schema);
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
          Objects.requireNonNull(schema),
          Objects.requireNonNull(options));
    }
  }
}
