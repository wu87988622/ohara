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

import com.island.ohara.common.data.Row;
import com.island.ohara.streams.data.Poneglyph;
import com.island.ohara.streams.ostream.*;
import java.util.List;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Serialized;
import org.apache.kafka.streams.kstream.ValueJoiner;

/**
 * {@code OStream} is a <i>Row</i> streaming data in Ohara Stream. In Ohara Stream environment, all
 * data is stored in topic ; Since we need to join streamApp in the data flow with other components
 * (for example: connector), the data type consistency is important. Use the same data type in
 * streamApp as same as connector which is {@code <Row, byte[]>} and we only use the <b>key</b>
 * part, leading us to restrict {@code OStream} should only do ETL work in {@code Row} data.
 *
 * @param <T> Type of value
 */
public interface OStream<T extends Row> {

  /**
   * Create a {@link OTable} from current {@code OStream}. All the configurations of the specify
   * topic will be same as {@code OStream}.
   *
   * @param topicName the topic name; cannot be {@code null}
   * @return {@link OTable}
   * @see org.apache.kafka.streams.StreamsBuilder#table(String, org.apache.kafka.streams.Consumed)
   */
  OTable<T> constructTable(String topicName);

  /**
   * Create a new {@code OStream} that filter by the given predicate. All records that do not
   * satisfy the predicate are dropped. This operation do not touch state store.
   *
   * @param predicate a filter {@link Predicate}
   * @return {@code OStream}
   * @see
   *     org.apache.kafka.streams.kstream.KStream#filter(org.apache.kafka.streams.kstream.Predicate)
   */
  OStream<T> filter(Predicate predicate);

  /**
   * Transfer this {@code OStream} to specify topic and use the required partition number. This
   * operation will do the repartition work.
   *
   * @param topicName the transfer topic name
   * @param partitions the partition size of topic
   * @return {@code OStream}
   * @see org.apache.kafka.streams.kstream.KStream#through(String,
   *     org.apache.kafka.streams.kstream.Produced)
   */
  OStream<T> through(String topicName, int partitions);

  /**
   * Join this stream with required topic using non-windowed left join. The join operation will use
   * the specify {@code Conditions} to lookup {@code stream.key == topic.key}.
   *
   * @param joinTopicName the topic name to be joined with this OStream
   * @param conditions the join key pairs
   * @param joiner a {@link ValueJoiner} that computes the join result for a pair of matching
   *     records
   * @return {@code OStream}
   * @see org.apache.kafka.streams.kstream.KStream#leftJoin(KTable, ValueJoiner)
   */
  OStream<T> leftJoin(String joinTopicName, Conditions conditions, Valuejoiner joiner);

  /**
   * Transform the value of each record to a new value of the output record. The provided {@link
   * ValueMapper} is applied to each input record value and computes a new output record value. This
   * operation do not touch state store.
   *
   * @param mapper a {@link ValueMapper} that computes a new output value
   * @return {@code OStream}
   * @see
   *     org.apache.kafka.streams.kstream.KStream#mapValues(org.apache.kafka.streams.kstream.ValueMapper)
   */
  OStream<T> map(ValueMapper mapper);

  /**
   * Group the records by key to a {@link OGroupedStream}.
   *
   * @param keys the group by key list
   * @return {@link OGroupedStream}
   * @see org.apache.kafka.streams.kstream.KStream#groupByKey(Serialized)
   */
  OGroupedStream<T> groupByKey(List<String> keys);

  /**
   * Perform an action on each record of {@code OStream}. This operation do not use state store.
   * Note that this is a terminal operation as {@link #start()}, {@link #describe()} and {@link
   * #getPoneglyph()}.
   *
   * @param action an action to perform on each record
   * @see
   *     org.apache.kafka.streams.kstream.KStream#foreach(org.apache.kafka.streams.kstream.ForeachAction)
   */
  void foreach(ForeachAction action);

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
