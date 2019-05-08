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
 * The {@code Reducer} interface for combining two values of the same type.
 *
 * @param <V> the record value type
 * @see org.apache.kafka.streams.kstream.Reducer
 */
public interface Reducer<V> {

  V reducer(V value1, V value2);

  class TrueReducer<V> implements org.apache.kafka.streams.kstream.Reducer<V> {

    private final Reducer<V> trueReducer;

    TrueReducer(Reducer<V> reducer) {
      this.trueReducer = reducer;
    }

    @Override
    public V apply(V value1, V value2) {
      return this.trueReducer.reducer(value1, value2);
    }
  }
}
