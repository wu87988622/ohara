package com.island.ohara.kafka;

import org.apache.kafka.common.config.TopicConfig;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * a helper class used to create the kafka topic. all member are protected since we have to
 * implement a do-nothing TopicCreator in testing.
 */
public abstract class TopicCreator {
  protected int numberOfPartitions = 1;
  protected short numberOfReplications = 1;
  protected Map<String, String> options = new HashMap<>();
  protected Duration timeout = Duration.ofSeconds(10);

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
    doOptions(
        Collections.singletonMap(
            TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_COMPACT),
        false);
    return this;
  }
  /**
   * Specify that the topic's data should be deleted. It means the topic won't keep any data when
   * cleanup
   *
   * @return this builder
   */
  public TopicCreator deleted() {
    doOptions(
        Collections.singletonMap(
            TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE),
        false);
    return this;
  }

  private TopicCreator doOptions(Map<String, String> options, boolean overwrite) {
    if (this.options == null || overwrite) {
      this.options = new HashMap<>(options);
    } else {
      this.options
          .entrySet()
          .stream()
          .filter(x -> options.containsKey(x.getKey()))
          .forEach(
              x -> {
                if (!options.get(x.getKey()).equals(x.getValue()))
                  throw new IllegalArgumentException(
                      String.format(
                          "conflict options! previous:%s new:%s",
                          x.getValue(), options.get(x.getKey())));
              });

      this.options.putAll(options);
    }
    return this;
  }

  public TopicCreator timeout(Duration timeout) {
    this.timeout = timeout;
    return this;
  }

  public abstract void create(String name);
}
