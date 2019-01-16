package com.island.ohara.streams.ostream;

import org.apache.kafka.common.serialization.Serde;

public class Produced<K, V> {

  private final Serde<K> keySerde;
  private final Serde<V> valueSerde;

  Produced(Serde key, Serde value) {
    this.keySerde = key;
    this.valueSerde = value;
  }

  org.apache.kafka.streams.kstream.Produced<K, V> get() {
    return org.apache.kafka.streams.kstream.Produced.with(this.keySerde, this.valueSerde);
  }
}
