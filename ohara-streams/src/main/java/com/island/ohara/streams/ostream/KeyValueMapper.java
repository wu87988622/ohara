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
 * The {@code KeyValueMapper} interface represents a {@link KeyValue key-value} pair function to
 * transform the input record to new record.
 *
 * @param <K> input record key type
 * @param <V> input record value type
 * @param <VR> {@link KeyValue} type
 * @see org.apache.kafka.streams.kstream.KeyValueMapper
 */
public interface KeyValueMapper<K, V, VR> {
  VR keyValueMapper(final K key, final V value);

  class TrueKeyValueMapper<K, V, VR>
      implements org.apache.kafka.streams.kstream.KeyValueMapper<K, V, VR> {

    private final KeyValueMapper<K, V, VR> trueKeyValueMapper;

    TrueKeyValueMapper(KeyValueMapper<K, V, VR> keyValueMapper) {
      this.trueKeyValueMapper = keyValueMapper;
    }

    @Override
    public VR apply(K key, V value) {
      return this.trueKeyValueMapper.keyValueMapper(key, value);
    }
  }
}
