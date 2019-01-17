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

public interface StreamPartitioner<K, V> {

  Integer partition(K key, V value, int numPartitions);

  class TrueStreamPartitioner<K, V>
      implements org.apache.kafka.streams.processor.StreamPartitioner<K, V> {

    private final StreamPartitioner<K, V> trueStreamPartitioner;

    TrueStreamPartitioner(StreamPartitioner<K, V> streamPartitioner) {
      this.trueStreamPartitioner = streamPartitioner;
    }

    @Override
    public Integer partition(K key, V value, int numPartitions) {
      return this.trueStreamPartitioner.partition(key, value, numPartitions);
    }
  }
}
