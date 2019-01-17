/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

  @Override
  void close();

  static ProducerBuilder builder() {
    return new ProducerBuilder();
  }
}
