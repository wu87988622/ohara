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
import com.island.ohara.streams.OStream;
import com.island.ohara.streams.OTable;
import com.island.ohara.streams.data.Poneglyph;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.Serialized;

class OStreamImpl<K, V> extends AbstractStream<K, V> implements OStream<K, V> {

  private Topology topology = null;

  OStreamImpl(OStreamBuilder ob) {
    super(ob);
  }

  OStreamImpl(OStreamBuilder ob, KStream<K, V> stream, StreamsBuilder builder) {
    super(ob, stream, builder);
  }

  @Override
  public <VO> OTable<K, VO> constructTable(
      String topicName, Serde<K> topicKey, Serde<VO> topicValue) {
    Objects.requireNonNull(topicName, "topic can not be null");
    KTable<K, VO> table = innerBuilder.table(topicName, new Consumed<>(topicKey, topicValue).get());

    return new OTableImpl<>(builder, table, innerBuilder);
  }

  @Override
  public OStream<K, V> filter(final Predicate<K, V> predicate) {
    Predicate.TruePredicate<K, V> truePredicate = new Predicate.TruePredicate<>(predicate);
    return new OStreamImpl<>(builder, kstreams.filter(truePredicate), innerBuilder);
  }

  @Override
  public OStream<K, V> through(String topicName, Serde<K> key, Serde<V> value) {
    Produced<K, V> produced = Produced.with(key, value);
    return new OStreamImpl<>(builder, kstreams.through(topicName, produced), innerBuilder);
  }

  @Override
  public <VO, VR> OStream<K, VR> leftJoin(
      String joinTopicName,
      Serde<K> topicKey,
      Serde<VO> topicValue,
      Valuejoiner<V, VO, VR> joiner) {
    Objects.requireNonNull(joinTopicName, "topic can not be null");
    KTable<K, VO> table =
        innerBuilder.table(joinTopicName, new Consumed<>(topicKey, topicValue).get());
    Valuejoiner.TrueValuejoiner<V, VO, VR> trueValuejoiner =
        new Valuejoiner.TrueValuejoiner<>(joiner);

    return new OStreamImpl<>(builder, kstreams.leftJoin(table, trueValuejoiner), innerBuilder);
  }

  @Override
  public <KR, VR> OStream<KR, VR> map(KeyValueMapper<K, V, KeyValue<KR, VR>> mapper) {
    KeyValueMapper.TrueKeyValueMapper<K, V, KeyValue<KR, VR>> trueKeyValueMapper =
        new KeyValueMapper.TrueKeyValueMapper<>(mapper);
    return new OStreamImpl<>(builder, kstreams.map(trueKeyValueMapper), innerBuilder);
  }

  @Override
  public <VR> OStream<K, VR> mapValues(final ValueMapper<V, VR> mapper) {
    ValueMapper.TrueValueMapper<V, VR> trueValueMapper = new ValueMapper.TrueValueMapper<>(mapper);
    return new OStreamImpl<>(builder, kstreams.mapValues(trueValueMapper), innerBuilder);
  }

  @Override
  public OGroupedStream<K, V> groupByKey(final Serde<K> key, final Serde<V> value) {
    Serialized<K, V> serialized = Serialized.with(key, value);
    return new OGroupedStreamImpl<>(builder, kstreams.groupByKey(serialized), innerBuilder);
  }

  /**
   * Initial topology object if not exists
   *
   * @param isDryRun describe only or not
   */
  private void baseActionInitial(boolean isDryRun) {
    if (topology == null) {
      Properties prop = new Properties();
      prop.setProperty(StreamsConfig.BOOTSTRAP_SERVERS, builder.getBootstrapServers());
      prop.setProperty(StreamsConfig.APP_ID, builder.getAppId());
      prop.setProperty(StreamsConfig.CLIENT_ID, builder.getAppId());
      prop.setProperty(StreamsConfig.DEFAULT_KEY_SERDE, Serdes.StringSerde.class.getName());
      prop.setProperty(StreamsConfig.DEFAULT_VALUE_SERDE, Serdes.RowSerde.class.getName());
      if (builder.getExtractor() != null) {
        prop.setProperty(StreamsConfig.TIMESTAMP_EXTRACTOR, builder.getExtractor().getName());
      }
      // We need to disable cache to get the aggregation result "immediately" ?...by Sam
      // Reference : https://docs.confluent.io/current/streams/developer-guide/memory-mgmt.html
      prop.setProperty(StreamsConfig.CACHE_BUFFER, "0");

      org.apache.kafka.streams.StreamsConfig config =
          new org.apache.kafka.streams.StreamsConfig(prop);

      topology = new Topology(innerBuilder, config, builder.isCleanStart(), isDryRun);
    }
  }

  @Override
  public void foreach(ForeachAction<K, V> action) {
    ForeachAction.TrueForeachAction<K, V> trueForeachAction =
        new ForeachAction.TrueForeachAction<>(action);
    kstreams.foreach(trueForeachAction);

    // Initial properties and topology for "actual" action
    baseActionInitial(false);

    topology.start();
  }

  @Override
  public void start() {
    kstreams.to(builder.getToTopic(), builder.getToSerde().get());

    // Initial properties and topology for "actual" action
    baseActionInitial(false);

    topology.start();
  }

  @Override
  public void stop() {
    if (topology == null) {
      throw new RuntimeException("The StreamApp : " + builder.getAppId() + " is not running");
    }
    topology.close();
  }

  @Override
  public String describe() {
    kstreams.to(builder.getToTopic(), builder.getToSerde().get());

    // Initial properties and topology for "actual" action
    baseActionInitial(true);

    return topology.describe();
  }

  @Override
  public List<Poneglyph> getPoneglyph() {
    kstreams.to(builder.getToTopic(), builder.getToSerde().get());

    // Initial properties and topology for "actual" action
    baseActionInitial(true);

    return topology.getPoneglyphs();
  }
}
