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

package com.island.ohara.streams.ostream;

/**
 * The {@code Predicate} interface represents a boolean-returned filter function. This function
 * should use to filter a {@link KeyValue} pair data.
 *
 * @param <K> key type
 * @param <V> value type
 * @see org.apache.kafka.streams.kstream.Predicate
 */
public interface Predicate<K, V> {

  boolean predicate(final K key, final V value);

  final class TruePredicate<K, V> implements org.apache.kafka.streams.kstream.Predicate<K, V> {
    final Predicate<K, V> truePredicate;

    TruePredicate(Predicate<K, V> predicate) {
      this.truePredicate = predicate;
    }

    @Override
    public boolean test(final K key, final V value) {
      return this.truePredicate.predicate(key, value);
    }
  }
}
