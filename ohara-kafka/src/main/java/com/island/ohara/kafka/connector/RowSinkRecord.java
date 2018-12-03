package com.island.ohara.kafka.connector;

import com.island.ohara.common.data.Row;

/**
 * The methods it have are almost same with SinkRecord. It return Table rather than any object.
 * Also, it doesn't have method to return value schema because the value schema is useless to user.
 *
 * @param sinkRecord a sink record passed by kafka connector
 */
public class RowSinkRecord {
  private final String topic;
  private final byte[] key;
  private final Row row;
  private final int partition;
  private final long offset;
  private final long timestamp;
  private final TimestampType timestampType;

  public RowSinkRecord(
      String topic,
      byte[] key,
      Row row,
      int partition,
      long offset,
      long timestamp,
      TimestampType timestampType) {
    this.topic = topic;
    this.key = key;
    this.row = row;
    this.partition = partition;
    this.offset = offset;
    this.timestamp = timestamp;
    this.timestampType = timestampType;
  }

  public String topic() {
    return topic;
  }

  public byte[] key() {
    return key;
  }

  public Row row() {
    return row;
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

  public TimestampType timestampType() {
    return timestampType;
  }
}
