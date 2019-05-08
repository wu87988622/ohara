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
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Serialized;
import org.apache.kafka.streams.kstream.ValueJoiner;

/**
 * {@code OStream} is a <i>key-value pair</i> streaming data
 *
 * @param <K> Type of key
 * @param <V> Type of value
 */
public interface OStream<K, V> {

  /**
   * Create a {@link OTable} from current {@code OStream}. All the configurations of the required
   * topic will as same as {@code OStream}.
   *
   * @param topicName the topic name; cannot be {@code null}
   * @param topicKey the topic key serialization; cannot be {@code null}
   * @param topicValue the topic value serialization; cannot be {@code null}
   * @param <VO> the value type of the table
   * @return {@link OTable}
   * @see org.apache.kafka.streams.StreamsBuilder#table(String, org.apache.kafka.streams.Consumed)
   */
  <VO> OTable<K, VO> constructTable(String topicName, Serde<K> topicKey, Serde<VO> topicValue);

  /**
   * Create a new {@code OStream} that filter by the given predicate. All records that do not
   * satisfy the predicate are dropped. This operation do not touch state store.
   *
   * @param predicate a filter {@link Predicate}
   * @return {@code OStream}
   * @see
   *     org.apache.kafka.streams.kstream.KStream#filter(org.apache.kafka.streams.kstream.Predicate)
   */
  OStream<K, V> filter(Predicate<K, V> predicate);

  /**
   * Transfer this {@code OStream} to specific topic and use the required serialization of {@link
   * Serde key serde}, {@link Serde value serde}. This operation will do the repartition work. Note:
   * The required topic should be manually created before it is used.
   *
   * @param topicName the transfer topic name
   * @param key the key serialization
   * @param value the value serialization
   * @return {@code OStream}
   * @see org.apache.kafka.streams.kstream.KStream#through(String,
   *     org.apache.kafka.streams.kstream.Produced)
   */
  OStream<K, V> through(String topicName, Serde<K> key, Serde<V> value);

  /**
   * Join this stream with required topic using non-windowed left join. The join operation will use
   * a primary key to lookup {@code stream.key == topic.key}.
   *
   * @param joinTopicName the topic name to be joined with this OStream
   * @param topicKey the join topic key serialization
   * @param topicValue the join topic value serialization
   * @param joiner a {@link ValueJoiner} that computes the join result for a pair of matching
   *     records
   * @param <VT> the value type of the require join topic
   * @param <VR> the value type of the result OStream
   * @return {@code OStream}
   * @see org.apache.kafka.streams.kstream.KStream#leftJoin(KTable, ValueJoiner)
   */
  <VT, VR> OStream<K, VR> leftJoin(
      String joinTopicName, Serde<K> topicKey, Serde<VT> topicValue, Valuejoiner<V, VT, VR> joiner);

  /**
   * Transform each record of the stream to a new record in the output stream. The provided {@link
   * KeyValueMapper} is applied to each input record and computes a new output record. This
   * operation do not touch state store.
   *
   * @param mapper a {@link KeyValueMapper} that computes a new output record
   * @param <KR> the key type of the output stream
   * @param <VR> the value type of the output stream
   * @return {@code OStream}
   * @see
   *     org.apache.kafka.streams.kstream.KStream#map(org.apache.kafka.streams.kstream.KeyValueMapper)
   */
  <KR, VR> OStream<KR, VR> map(KeyValueMapper<K, V, KeyValue<KR, VR>> mapper);

  /**
   * Transform the value of each record to a new value of the output record. The provided {@link
   * ValueMapper} is applied to each input record value and computes a new output record value. This
   * operation do not touch state store.
   *
   * @param mapper a {@link ValueMapper} that computes a new output value
   * @param <VR> the value type of the output stream
   * @return {@code OStream}
   * @see
   *     org.apache.kafka.streams.kstream.KStream#mapValues(org.apache.kafka.streams.kstream.ValueMapper)
   */
  <VR> OStream<K, VR> mapValues(ValueMapper<V, VR> mapper);

  /**
   * Group the records by key to a {@link OGroupedStream} and using the serialization as specified
   * {@code key} and {@code value}.
   *
   * @param key the key type of the output stream
   * @param value the value type of the output stream
   * @return {@link OGroupedStream}
   * @see org.apache.kafka.streams.kstream.KStream#groupByKey(Serialized)
   */
  OGroupedStream<K, V> groupByKey(final Serde<K> key, final Serde<V> value);

  /**
   * Perform an action on each record of {@code OStream}. This operation do not use state store.
   * Note that this is a terminal operation as {@link #start()}, {@link #describe()} and {@link
   * #getPoneglyph()}.
   *
   * @param action an action to perform on each record
   * @see
   *     org.apache.kafka.streams.kstream.KStream#foreach(org.apache.kafka.streams.kstream.ForeachAction)
   */
  void foreach(ForeachAction<K, V> action);

  /**
   * Run this streamApp application. This operation do not use state store. Note that this is a
   * terminal operation as {@link #foreach(ForeachAction)}, {@link #describe()} and {@link
   * #getPoneglyph()}.
   */
  void start();

  /** Stop this streamApp application. */
  void stop();

  /**
   * Describe the topology of this streamApp. Note that this is a terminal operation as {@link
   * #foreach(ForeachAction)}, {@link #start()} and {@link #getPoneglyph()}.
   *
   * @return string of the {@code topology}
   */
  String describe();

  /**
   * Get the Ohara format {@link Poneglyph} from topology. Note that this is a terminal operation as
   * {@link #foreach(ForeachAction)}, {@link #start()} and {@link #describe()}.
   *
   * @return the {@link Poneglyph} list
   */
  List<Poneglyph> getPoneglyph();

  /**
   * Create a builder to define a {@code OStream}.
   *
   * @return the builder
   */
  static OStreamBuilder<byte[], byte[]> builder() {
    // By default, we use the <byte[], byte[]> type for generic as the stream topic consume with
    return new OStreamBuilder<>(Serdes.BYTES, Serdes.BYTES);
  }
}
