package com.island.ohara.streams.ostream;

import com.island.ohara.streams.OStream;

public interface OTable<K, V> {

  OStream<K, V> toOStream();
}
