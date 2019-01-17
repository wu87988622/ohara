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

import com.island.ohara.common.util.CommonUtil;
import com.island.ohara.common.util.Releasable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * a simple scala wrap from kafka consumer.
 *
 * @param <K></K> key type
 * @param <V></V> value type
 */
public interface Consumer<K, V> extends Releasable {

  /**
   * poll the data from subscribed topics
   *
   * @param timeout waiting time
   * @return records
   */
  List<ConsumerRecord<K, V>> poll(Duration timeout);

  /**
   * Overloading poll method
   *
   * @param timeout waiting time
   * @param expectedSize expected size
   * @return ConsumerRecords
   */
  default List<ConsumerRecord<K, V>> poll(Duration timeout, int expectedSize) {
    return poll(timeout, expectedSize, () -> false);
  }

  default List<ConsumerRecord<K, V>> poll(
      Duration timeout, int expectedSize, Supplier<Boolean> stop) {
    return poll(timeout, expectedSize, stop, Function.identity());
  }

  default List<ConsumerRecord<K, V>> poll(
      Duration timeout,
      int expectedSize,
      Function<List<ConsumerRecord<K, V>>, List<ConsumerRecord<K, V>>> filter) {
    return poll(timeout, expectedSize, () -> false, filter);
  }
  /**
   * It accept another condition - expected size from records. Somethins it is helpful if you
   * already know the number from records which should be returned.
   *
   * @param timeout timeout
   * @param expectedSize the number from records should be returned
   */
  default List<ConsumerRecord<K, V>> poll(
      Duration timeout,
      int expectedSize,
      Supplier<Boolean> stop,
      Function<List<ConsumerRecord<K, V>>, List<ConsumerRecord<K, V>>> filter) {

    List<ConsumerRecord<K, V>> list;
    if (expectedSize == Integer.MAX_VALUE) list = new ArrayList<>();
    else list = new ArrayList<>(expectedSize);

    long endtime = CommonUtil.current() + timeout.toMillis();
    long ramaining = endtime - CommonUtil.current();

    while (!stop.get() && list.size() < expectedSize && ramaining > 0) {
      list.addAll(filter.apply(poll(Duration.ofMillis(ramaining))));
      ramaining = endtime - CommonUtil.current();
    }
    return list;
  }

  /** @return the topic names subscribed by this consumer */
  Set<String> subscription();

  /** break the poll right now. */
  void wakeup();

  static ConsumerBuilder builder() {
    return new ConsumerBuilder();
  }
}
