package com.island.ohara.streams.ostream;

public interface KeyValueMapper<K, V, VR> {
  VR keyValueMapper(final K key, final V value);

  class TrueKeyValueMapper<K, V, VR>
      implements org.apache.kafka.streams.kstream.KeyValueMapper<K, V, VR> {

    private final KeyValueMapper<K, V, VR> trueKeyValueMapper;

    TrueKeyValueMapper(KeyValueMapper<K, V, VR> keyValueMapper) {
      this.trueKeyValueMapper = keyValueMapper;
    }

    @Override
    public VR apply(K key, V value) {
      return this.trueKeyValueMapper.keyValueMapper(key, value);
    }
  }
}
