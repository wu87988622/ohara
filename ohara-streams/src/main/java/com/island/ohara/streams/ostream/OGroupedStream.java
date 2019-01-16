package com.island.ohara.streams.ostream;

public interface OGroupedStream<K, V> {

  OTable<K, Long> count();

  OTable<K, V> reduce(final Reducer<V> reducer);
}
