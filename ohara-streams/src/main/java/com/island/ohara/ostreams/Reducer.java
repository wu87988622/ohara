package com.island.ohara.ostreams;

public interface Reducer<V> {

  V reducer(V value1, V value2);

  class TrueReducer<V> implements org.apache.kafka.streams.kstream.Reducer<V> {

    private final Reducer<V> trueReducer;

    TrueReducer(Reducer<V> reducer) {
      this.trueReducer = reducer;
    }

    @Override
    public V apply(V value1, V value2) {
      return this.trueReducer.reducer(value1, value2);
    }
  }
}
