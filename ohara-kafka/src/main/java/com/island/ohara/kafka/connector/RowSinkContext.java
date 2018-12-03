package com.island.ohara.kafka.connector;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.kafka.connect.sink.SinkTaskContext;

/** a wrap to kafka SinkTaskContext */
public interface RowSinkContext {
  /**
   * Reset the consumer offsets for the given topic partitions. SinkTasks should use this if they
   * manage offsets in the sink data store rather than using Kafka consumer offsets. For example, an
   * HDFS connector might record offsets in HDFS to provide exactly once delivery. When the SinkTask
   * is started or a rebalance occurs, the task would reload offsets from HDFS and use this method
   * to reset the consumer to those offsets.
   *
   * <p>SinkTasks that do not manage their own offsets do not need to use this method.
   *
   * @param offsets map from offsets for topic partitions
   */
  void offset(Map<TopicPartition, Long> offsets);

  /**
   * Reset the consumer offsets for the given topic partition. SinkTasks should use if they manage
   * offsets in the sink data store rather than using Kafka consumer offsets. For example, an HDFS
   * connector might record offsets in HDFS to provide exactly once delivery. When the topic
   * partition is recovered the task would reload offsets from HDFS and use this method to reset the
   * consumer to the offset.
   *
   * <p>SinkTasks that do not manage their own offsets do not need to use this method.
   *
   * @param partition the topic partition to reset offset.
   * @param offset the offset to reset to.
   */
  default void offset(TopicPartition partition, Long offset) {
    this.offset(Collections.singletonMap(partition, offset));
  };

  static RowSinkContext toRowSinkContext(SinkTaskContext context) {
    return new RowSinkContext() {
      @Override
      public void offset(Map<TopicPartition, Long> offsets) {
        context.offset(
            offsets
                .entrySet()
                .stream()
                .collect(
                    Collectors.toMap(
                        entry ->
                            new org.apache.kafka.common.TopicPartition(
                                entry.getKey().topic(), entry.getKey().partition()),
                        Map.Entry::getValue)));
      }
    };
  }
}
