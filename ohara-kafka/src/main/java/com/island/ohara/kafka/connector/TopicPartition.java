package com.island.ohara.kafka.connector;

import java.util.Objects;

/**
 * TopicPartition replaces kafka TopicPartition
 *
 * @see org.apache.kafka.common.TopicPartition
 */
public class TopicPartition {
  private final String topic;
  private final int partition;

  public TopicPartition(String topic, int partition) {
    this.topic = topic;
    this.partition = partition;
  }

  public String topic() {
    return topic;
  }

  public int partition() {
    return partition;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TopicPartition that = (TopicPartition) o;
    return partition == that.partition && Objects.equals(topic, that.topic);
  }

  @Override
  public int hashCode() {
    return Objects.hash(topic, partition);
  }
}
