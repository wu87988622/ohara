package com.island.ohara.kafka;

import java.util.List;
import java.util.Optional;
import org.apache.kafka.clients.consumer.ConsumerRecords;

/**
 * a scala wrap from kafka's consumer record.
 *
 * @see ConsumerRecords <K, V>
 * @param <K> K key type
 * @param <V> V value type
 */
public class ConsumerRecord<K, V> {
  private final String topic;
  private final List<Header> headers;
  private final Optional<K> key;
  private final Optional<V> value;

  /**
   * @param topic topic name
   * @param key key (nullable)
   * @param value value
   * @param K key type
   * @param V value type
   */
  public ConsumerRecord(String topic, List<Header> headers, Optional<K> key, Optional<V> value) {
    this.topic = topic;
    this.headers = headers;
    this.key = key;
    this.value = value;
  }

  public String topic() {
    return topic;
  }

  public List<Header> headers() {
    return headers;
  }

  public Optional<K> key() {
    return key;
  }

  public Optional<V> value() {
    return value;
  }
}
