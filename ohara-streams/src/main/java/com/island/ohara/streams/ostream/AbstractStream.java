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

import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KGroupedStream;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;

abstract class AbstractStream<K, V> {

  KTable<K, V> ktable;
  KStream<K, V> kstreams;
  KGroupedStream<K, V> kgroupstream;
  OStreamBuilder<K, V> builder;
  StreamsBuilder innerBuilder;

  @SuppressWarnings("unchecked")
  AbstractStream(final OStreamBuilder builder) {
    StreamsBuilder newBuilder = new StreamsBuilder();
    this.kstreams = newBuilder.stream(builder.getFromTopic(), builder.getFromSerde().get());
    this.builder = builder;
    this.innerBuilder = newBuilder;
  }

  AbstractStream(
      final OStreamBuilder builder, final KStream<K, V> kstreams, StreamsBuilder innerBuilder) {
    this.builder = builder;
    this.kstreams = kstreams;
    this.innerBuilder = innerBuilder;
  }

  AbstractStream(
      final OStreamBuilder builder,
      final KGroupedStream<K, V> kgroupstream,
      StreamsBuilder innerBuilder) {
    this.builder = builder;
    this.kgroupstream = kgroupstream;
    this.innerBuilder = innerBuilder;
  }

  AbstractStream(
      final OStreamBuilder builder, final KTable<K, V> ktable, StreamsBuilder innerBuilder) {
    this.builder = builder;
    this.ktable = ktable;
    this.innerBuilder = innerBuilder;
  }
}
