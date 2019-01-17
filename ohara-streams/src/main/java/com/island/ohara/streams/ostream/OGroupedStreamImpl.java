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

import com.island.ohara.streams.OGroupedStream;
import com.island.ohara.streams.OTable;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KGroupedStream;

@SuppressWarnings({"rawtypes", "unchecked"})
public class OGroupedStreamImpl<K, V> extends AbstractStream<K, V> implements OGroupedStream<K, V> {

  OGroupedStreamImpl(OStreamBuilder ob, KGroupedStream<K, V> kgroupstream, StreamsBuilder builder) {
    super(ob, kgroupstream, builder);
  }

  @Override
  public OTable<K, Long> count() {
    return new OTableImpl<>(builder, kgroupstream.count(), innerBuilder);
  }

  @Override
  public OTable<K, V> reduce(final Reducer<V> reducer) {
    Reducer.TrueReducer<V> trueReducer = new Reducer.TrueReducer<>(reducer);
    return new OTableImpl<>(builder, kgroupstream.reduce(trueReducer), innerBuilder);
  }
}
