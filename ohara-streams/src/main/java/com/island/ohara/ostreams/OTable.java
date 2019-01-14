package com.island.ohara.ostreams;

import com.island.ohara.OStream;

public interface OTable<K, V> {

  OStream<K, V> toOStream();
}
