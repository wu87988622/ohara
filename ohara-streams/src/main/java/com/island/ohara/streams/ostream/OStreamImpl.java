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

import com.island.ohara.common.data.Cell;
import com.island.ohara.common.data.Pair;
import com.island.ohara.common.data.Row;
import com.island.ohara.common.util.CommonUtils;
import com.island.ohara.kafka.BrokerClient;
import com.island.ohara.metrics.basic.Counter;
import com.island.ohara.streams.OGroupedStream;
import com.island.ohara.streams.OStream;
import com.island.ohara.streams.OTable;
import com.island.ohara.streams.data.Poneglyph;
import com.island.ohara.streams.metric.MetricFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"rawtypes", "unchecked"})
class OStreamImpl extends AbstractStream<Row, Row> implements OStream<Row> {

  private final Logger log = LoggerFactory.getLogger(OStreamImpl.class);
  private Topology topology = null;
  private static final Counter counter = MetricFactory.getCounter(MetricFactory.IOType.TOPIC_OUT);

  OStreamImpl(OStreamBuilder ob) {
    super(ob);
  }

  OStreamImpl(OStreamBuilder ob, KStream<Row, Row> stream, StreamsBuilder builder) {
    super(ob, stream, builder);
  }

  @Override
  public OTable<Row> constructTable(String topicName) {
    Objects.requireNonNull(topicName, "topicName can not be null");
    KTable<Row, Row> table =
        innerBuilder.table(topicName, new Consumed<>(Serdes.ROW, Serdes.ROW).get());

    return new OTableImpl(builder, table, innerBuilder);
  }

  @Override
  public OStream<Row> filter(final Predicate predicate) {
    Predicate.TruePredicate truePredicate = new Predicate.TruePredicate(predicate);
    return new OStreamImpl(builder, kstreams.filter(truePredicate), innerBuilder);
  }

  @Override
  public OStream<Row> through(String topicName, int partitions) {
    BrokerClient client = BrokerClient.of(builder.getBootstrapServers());
    client.topicCreator().topicName(topicName).numberOfPartitions(partitions).create();
    return new OStreamImpl(
        builder, kstreams.through(topicName, Produced.with(Serdes.ROW, Serdes.ROW)), innerBuilder);
  }

  @Override
  public OStream<Row> leftJoin(
      String joinTopicName, Conditions conditions, ValueJoiner valueJoiner) {
    CommonUtils.requireNonEmpty(joinTopicName, () -> "joinTopicName cannot be null");
    // construct the compare key "row"
    List<Pair<String, String>> list = conditions.getConditionList();
    CommonUtils.requireNonEmpty(list, () -> "the conditions cannot be empty");

    List<String> leftHeaders = new ArrayList<>();
    List<String> rightHeaders = new ArrayList<>();
    for (Pair<String, String> pair : list) {
      leftHeaders.add(pair.left());
      rightHeaders.add(pair.right());
    }

    // convert the right topic (the join topic) to <Row: key_header, Row: values>
    KTable<Row, Row> table =
        innerBuilder.stream(joinTopicName, new Consumed<>(Serdes.ROW, Serdes.BYTES).get())
            .map(
                (row, value) ->
                    new KeyValue<>(
                        Row.of(
                            rightHeaders.stream()
                                .map(
                                    name ->
                                        Cell.of(
                                            list.get(rightHeaders.indexOf(name)).left(),
                                            row.cell(name).value()))
                                .toArray(Cell[]::new)),
                        row))
            .groupByKey(Grouped.with(Serdes.ROW, Serdes.ROW))
            .reduce((agg, newValue) -> newValue);

    // convert the left topic (this stream) to <Row: key_header_value, Row: values>
    // do left join
    return new OStreamImpl(
        builder,
        kstreams
            .map(
                (row, value) ->
                    new KeyValue<>(
                        Row.of(leftHeaders.stream().map(value::cell).toArray(Cell[]::new)), value))
            .leftJoin(table, valueJoiner::apply),
        innerBuilder);
  }

  @Override
  public OStream<Row> map(final ValueMapper mapper) {
    ValueMapper.TrueValueMapper trueValueMapper = new ValueMapper.TrueValueMapper(mapper);
    return new OStreamImpl(builder, kstreams.mapValues(trueValueMapper), innerBuilder);
  }

  @Override
  public OGroupedStream<Row> groupByKey(List<String> keys) {
    CommonUtils.requireNonEmpty(keys, () -> "the conditions cannot be empty");

    return new OGroupedStreamImpl(
        builder,
        kstreams
            .map(
                (row, value) ->
                    new KeyValue<>(
                        Row.of(keys.stream().map(value::cell).toArray(Cell[]::new)), value))
            .groupByKey(),
        innerBuilder);
  }

  /**
   * Initial topology object if not exists
   *
   * @param isDryRun describe only or not
   */
  private void baseActionInitial(boolean isDryRun) {
    if (topology == null) {
      Properties prop = new Properties();
      // TODO : enable exactly_once in #1116
      // Enable exactly_once prop.setProperty(StreamsConfig.GUARANTEE,
      // StreamsConfig.GUARANTEES.EXACTLY_ONCE.getName());

      prop.setProperty(StreamsConfig.BOOTSTRAP_SERVERS, builder.getBootstrapServers());
      prop.setProperty(StreamsConfig.APP_ID, builder.getAppId());
      prop.setProperty(StreamsConfig.CLIENT_ID, builder.getAppId());
      // Since we convert to <row, row> data type for internal ostream usage
      prop.setProperty(StreamsConfig.DEFAULT_KEY_SERDE, Serdes.RowSerde.class.getName());
      prop.setProperty(StreamsConfig.DEFAULT_VALUE_SERDE, Serdes.RowSerde.class.getName());
      if (builder.getExtractor() != null) {
        prop.setProperty(StreamsConfig.TIMESTAMP_EXTRACTOR, builder.getExtractor().getName());
      }
      // We need to disable cache to get the aggregation result "immediately" ?...by Sam
      // Reference : https://docs.confluent.io/current/streams/developer-guide/memory-mgmt.html
      prop.setProperty(StreamsConfig.CACHE_BUFFER, "0");

      topology = new Topology(innerBuilder, prop, builder.isCleanStart(), isDryRun);
      log.info(String.format("poneglyph:%s", topology.getPoneglyphs().toString()));
    }
  }

  @Override
  public void foreach(ForeachAction action) {
    ForeachAction.TrueForeachAction trueForeachAction = new ForeachAction.TrueForeachAction(action);
    kstreams.map(((noUse, value) -> KeyValue.pair(value, new byte[0]))).foreach(trueForeachAction);

    // Initial properties and topology for "actual" action
    baseActionInitial(false);

    topology.start();
  }

  @Override
  public void start() {
    kstreams
        .map(
            ((noUse, value) -> {
              // we calculate the output record size
              counter.incrementAndGet();
              return KeyValue.pair(value, new byte[0]);
            }))
        .to(builder.getToTopic(), builder.getToSerde().get());

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
    kstreams
        .map(((noUse, value) -> KeyValue.pair(value, new byte[0])))
        .to(builder.getToTopic(), builder.getToSerde().get());

    // Initial properties and topology for "actual" action
    baseActionInitial(true);

    return topology.describe();
  }

  @Override
  public List<Poneglyph> getPoneglyph() {
    kstreams
        .map(((noUse, value) -> KeyValue.pair(value, new byte[0])))
        .to(builder.getToTopic(), builder.getToSerde().get());

    // Initial properties and topology for "actual" action
    baseActionInitial(true);

    return topology.getPoneglyphs();
  }
}
