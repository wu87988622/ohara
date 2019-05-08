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

package com.island.ohara.streams;

import com.island.ohara.streams.ostream.Reducer;

/**
 * {@code OGroupedStream} is a <i>grouped stream</i> by key.
 *
 * @param <K> Type of the key
 * @param <V> Type of the value
 */
public interface OGroupedStream<K, V> {

  /**
   * Count the number of records in this {@code OStream}.
   *
   * @return {@code OStream}
   * @see org.apache.kafka.streams.kstream.KGroupedStream#count()
   */
  OStream<K, Long> count();

  /**
   * Combine the values of each record in the {@code OStream} by the grouped key.
   *
   * @param reducer a{@link Reducer} that computes a new aggregate result.
   * @return {@code OStream}
   * @see
   *     org.apache.kafka.streams.kstream.KGroupedStream#reduce(org.apache.kafka.streams.kstream.Reducer)
   */
  OStream<K, V> reduce(final Reducer<V> reducer);
}
