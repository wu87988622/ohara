package com.island.ohara.kafka.connector;

import com.island.ohara.client.ConfiguratorJson.Column;
import java.util.List;
import java.util.Map;

/** this class carries all required configs for row connectors. */
public class TaskConfig {

  public static TaskConfig apply(
      String name, List<String> topics, List<Column> schema, Map<String, String> options) {
    return new TaskConfig(name, topics, schema, options);
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
  public TaskConfig(
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
}
