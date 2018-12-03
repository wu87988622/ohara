package com.island.ohara.kafka;

/**
 * wrap from kafka RecordMetadata;
 *
 * @see org.apache.kafka.clients.producer.RecordMetadata;
 */
public class RecordMetadata {
  private final String topic;
  private final int partition;
  private final long offset;
  private final long timestamp;
  private final int serializedKeySize;
  private final int serializedValueSize;

  public RecordMetadata(
      String topic,
      int partition,
      long offset,
      long timestamp,
      int serializedKeySize,
      int serializedValueSize) {
    this.topic = topic;
    this.partition = partition;
    this.offset = offset;
    this.timestamp = timestamp;
    this.serializedKeySize = serializedKeySize;
    this.serializedValueSize = serializedValueSize;
  }

  public String topic() {
    return topic;
  }

  public int partition() {
    return partition;
  }

  public long offset() {
    return offset;
  }

  public long timestamp() {
    return timestamp;
  }

  public int serializedKeySize() {
    return serializedKeySize;
  }

  public int serializedValueSize() {
    return serializedValueSize;
  }
}
