package com.island.ohara.kafka.connector;

/**
 * TopicPartition replaces Kafka OffsetAndMetadata
 *
 * @see org.apache.kafka.clients.consumer.OffsetAndMetadata
 */
public class TopicOffset {
  private final String metadata;
  private final Long offset;

  public TopicOffset(String metadata, Long offset) {
    this.metadata = metadata;
    this.offset = offset;
  }

  public String metadata() {
    return metadata;
  }

  public Long offset() {
    return offset;
  }
}
