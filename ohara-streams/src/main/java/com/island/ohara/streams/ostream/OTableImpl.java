package com.island.ohara.streams.ostream;

import com.island.ohara.streams.OStream;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KTable;

public class OTableImpl<K, V> extends AbstractStream<K, V> implements OTable<K, V> {
  OTableImpl(OStreamBuilder ob, KTable<K, V> ktable, StreamsBuilder builder) {
    super(ob, ktable, builder);
  }

  @Override
  public OStream<K, V> toOStream() {
    return new OStreamImpl<>(ob, ktable.toStream(), builder);
  }
}
