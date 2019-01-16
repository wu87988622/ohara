package com.island.ohara.streams.ostream;

public interface ForeachAction<K, V> {

  void foreachAction(final K key, final V value);

  class TrueForeachAction<K, V> implements org.apache.kafka.streams.kstream.ForeachAction<K, V> {

    private final ForeachAction<K, V> trueForeachAction;

    TrueForeachAction(ForeachAction<K, V> foreachAction) {
      this.trueForeachAction = foreachAction;
    }

    @Override
    public void apply(K key, V value) {
      this.trueForeachAction.foreachAction(key, value);
    }
  }
}
