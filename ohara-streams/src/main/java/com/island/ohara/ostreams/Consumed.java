package com.island.ohara.ostreams;

public class Consumed<K, V> {

  private final Serde<K> keySerde;
  private final Serde<V> valueSerde;

  Consumed(Serde key, Serde value) {
    this.keySerde = key;
    this.valueSerde = value;
  }

  org.apache.kafka.streams.Consumed<K, V> get() {
    return org.apache.kafka.streams.Consumed.with(this.keySerde, this.valueSerde);
  }
}
