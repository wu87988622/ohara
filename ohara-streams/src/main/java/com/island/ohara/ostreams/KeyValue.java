package com.island.ohara.ostreams;

public class KeyValue<K, V> extends org.apache.kafka.streams.KeyValue<K, V> {
  // TODO : could we not extends the kafka class ?...by Sam

  /**
   * Create a new key-value pair.
   *
   * @param key the key
   * @param value the value
   */
  public KeyValue(K key, V value) {
    super(key, value);
  }
}
