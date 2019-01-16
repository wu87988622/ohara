package com.island.ohara.streams;

import com.island.ohara.common.data.Row;
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

  static OStreamBuilder<String, Row> builder() {
    // By default, we use the <String, Row> as the stream topic consume with
    return new OStreamBuilder<>(Serdes.StringSerde, Serdes.RowSerde);
  }
}
