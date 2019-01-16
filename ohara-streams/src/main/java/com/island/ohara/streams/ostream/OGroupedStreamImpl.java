package com.island.ohara.streams.ostream;

import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KGroupedStream;

public class OGroupedStreamImpl<K, V> extends AbstractStream<K, V> implements OGroupedStream<K, V> {

  OGroupedStreamImpl(OStreamBuilder ob, KGroupedStream<K, V> kgroupstream, StreamsBuilder builder) {
    super(ob, kgroupstream, builder);
  }

  @Override
  public OTable<K, Long> count() {
    return new OTableImpl<>(ob, kgroupstream.count(), builder);
  }

  @Override
  public OTable<K, V> reduce(final Reducer<V> reducer) {
    Reducer.TrueReducer<V> trueReducer = new Reducer.TrueReducer<>(reducer);
    return new OTableImpl<>(ob, kgroupstream.reduce(trueReducer), builder);
  }
}
