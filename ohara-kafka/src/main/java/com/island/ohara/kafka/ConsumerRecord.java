package com.island.ohara.kafka;

import com.island.ohara.common.util.CommonUtil;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.kafka.clients.consumer.ConsumerRecords;

/**
 * a scala wrap from kafka's consumer record.
 *
 * @see ConsumerRecords <K, V>
 * @param <K> K key type
 * @param <V> V value type
 */
public final class ConsumerRecord<K, V> {
  private final String topic;
  private final List<Header> headers;
  private final K key;
  private final V value;

  /**
   * @param topic topic name
   * @param key key (nullable)
   * @param value value
   */
  ConsumerRecord(String topic, List<Header> headers, K key, V value) {
    this.topic = topic;
    this.headers = Collections.unmodifiableList(headers);
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
    return Optional.ofNullable(key);
  }

  public Optional<V> value() {
    return Optional.ofNullable(value);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ConsumerRecord<?, ?> that = (ConsumerRecord<?, ?>) o;
    return Objects.equals(topic, that.topic)
        && CommonUtil.equals(headers, that.headers)
        && Objects.equals(key, that.key)
        && Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(topic, headers, key, value);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("topic", topic)
        .append("headers", headers)
        .append("key", key)
        .append("value", value)
        .toString();
  }
}
