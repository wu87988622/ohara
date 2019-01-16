package com.island.ohara.streams.ostream;

public interface ValueMapper<V, VR> {

  VR valueMapper(V value);

  class TrueValueMapper<V, VR> implements org.apache.kafka.streams.kstream.ValueMapper<V, VR> {

    private final ValueMapper<V, VR> trueValueMapper;

    TrueValueMapper(ValueMapper<V, VR> valueMapper) {
      this.trueValueMapper = valueMapper;
    }

    @Override
    public VR apply(V value) {
      return this.trueValueMapper.valueMapper(value);
    }
  }
}
