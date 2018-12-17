package com.island.ohara.kafka;

import com.island.ohara.common.util.Releasable;

/**
 * a simple wrap from kafka producer.
 *
 * @param <K> key type
 * @param <V> value type
 */
public interface Producer<K, V> extends Releasable {

  /**
   * create a sender used to send a record to brokers
   *
   * @return a sender
   */
  Sender<K, V> sender();

  /** flush all on-the-flight data. */
  void flush();

  static ProducerBuilder builder() {
    return new ProducerBuilder();
  }
}
