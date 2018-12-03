package com.island.ohara.kafka;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.common.config.TopicConfig;

/**
 * a helper class used to create the kafka topic. all member are protected since we have to
 * implement a do-nothing TopicCreator in testing.
 */
public abstract class TopicCreator {
  private int numberOfPartitions = 1;
  private short numberOfReplications = 1;
  private Map<String, String> options = new HashMap<>();
  private Duration timeout = Duration.ofSeconds(10);

  protected int numberOfPartitions() {
    return numberOfPartitions;
  }

  protected short numberOfReplications() {
    return numberOfReplications;
  }

  protected Map<String, String> options() {
    return options;
  }

  protected Duration timeout() {
    return timeout;
  }

  public TopicCreator numberOfPartitions(int numberOfPartitions) {
    this.numberOfPartitions = numberOfPartitions;
    return this;
  }

  public TopicCreator numberOfReplications(short numberOfReplications) {
    this.numberOfReplications = numberOfReplications;
    return this;
  }

  public TopicCreator options(Map<String, String> options) {
    doOptions(options, true);
    return this;
  }

  /**
   * Specify that the topic's data should be compacted. It means the topic will keep the latest
   * value for each key.
   *
   * @return this builder
   */
  public TopicCreator compacted() {
    Map<String, String> map = new HashMap<>();
    map.put(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_COMPACT);
    doOptions(map, false);
    return this;
  }
  /**
   * Specify that the topic's data should be deleted. It means the topic won't keep any data when
   * cleanup
   *
   * @return this builder
   */
  public TopicCreator deleted() {
    Map<String, String> map = new HashMap<>();
    map.put(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE);
    doOptions(map, false);
    return this;
  }

  private TopicCreator doOptions(Map<String, String> options, boolean overwrite) {
    if (this.options == null || overwrite) {
      Map<String, String> map = new HashMap<>();
      map.putAll(options);
      this.options = map;
    } else {
      this.options
          .entrySet()
          .stream()
          .filter(x -> options.containsKey(x.getKey()))
          .forEach(
              x -> {
                if (options.get(x.getKey()) != x.getValue())
                  throw new IllegalArgumentException(
                      String.format(
                          "conflict options! previous:%s new:%s",
                          x.getValue(), options.get(x.getKey())));
              });
      options.forEach(
          (k, v) -> {
            this.options.put(k, v);
          });
    }
    return this;
  }

  public TopicCreator timeout(Duration timeout) {
    this.timeout = timeout;
    return this;
  }

  public void create(String name) {
    doCreate(name);
  };

  protected abstract void doCreate(String name);
}
