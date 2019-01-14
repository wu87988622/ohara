package com.island.ohara.ostreams;

public interface Serde<T> extends org.apache.kafka.common.serialization.Serde<T> {
  // TODO : Serde has been wrapped, currently we have to extends the kakfa Serde...by Sam
}
