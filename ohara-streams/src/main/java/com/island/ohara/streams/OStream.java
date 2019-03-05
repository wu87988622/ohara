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

import com.island.ohara.streams.data.Poneglyph;
import com.island.ohara.streams.ostream.*;
import java.util.List;

public interface OStream<K, V> {

  <VO> OTable<K, VO> constructTable(String topicName, Serde<K> topicKey, Serde<VO> topicValue);

  OStream<K, V> filter(Predicate<K, V> predicate);

  OStream<K, V> through(String topicName, Serde<K> key, Serde<V> value);

  <VO, VR> OStream<K, VR> leftJoin(
      String joinTopicName, Serde<K> topicKey, Serde<VO> topicValue, Valuejoiner<V, VO, VR> joiner);

  <KR, VR> OStream<KR, VR> map(KeyValueMapper<K, V, KeyValue<KR, VR>> mapper);

  <VR> OStream<K, VR> mapValues(ValueMapper<V, VR> mapper);

  OGroupedStream<K, V> groupByKey(final Serde<K> key, final Serde<V> value);

  void foreach(ForeachAction<K, V> action);

  void start();

  void stop();

  String describe();

  List<Poneglyph> getPoneglyph();

  static OStreamBuilder<byte[], byte[]> builder() {
    // By default, we use the <byte[], byte[]> type for generic as the stream topic consume with
    return new OStreamBuilder<>(Serdes.BYTES, Serdes.BYTES);
  }
}
