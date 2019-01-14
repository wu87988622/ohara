package com.island.ohara.ostreams;

import com.island.ohara.OStream;
import com.island.ohara.data.Poneglyph;
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
    KTable<K, VO> table = builder.table(topicName, new Consumed<K, VO>(topicKey, topicValue).get());

    return new OTableImpl<>(ob, table, builder);
  }

  @Override
  public OStream<K, V> filter(final Predicate<K, V> predicate) {
    Predicate.TruePredicate<K, V> truePredicate = new Predicate.TruePredicate<>(predicate);
    return new OStreamImpl<>(ob, kstreams.filter(truePredicate), builder);
  }

  @Override
  public OStream<K, V> through(String topicName, Serde<K> key, Serde<V> value) {
    Produced<K, V> produced = Produced.with(key, value);
    return new OStreamImpl<>(ob, kstreams.through(topicName, produced), builder);
  }

  @Override
  public <VO, VR> OStream<K, VR> leftJoin(
      String joinTopicName,
      Serde<K> topicKey,
      Serde<VO> topicValue,
      Valuejoiner<V, VO, VR> joiner) {
    Objects.requireNonNull(joinTopicName, "topic can not be null");
    KTable<K, VO> table =
        builder.table(joinTopicName, new Consumed<K, VO>(topicKey, topicValue).get());
    Valuejoiner.TrueValuejoiner<V, VO, VR> trueValuejoiner =
        new Valuejoiner.TrueValuejoiner<>(joiner);

    return new OStreamImpl<>(ob, kstreams.leftJoin(table, trueValuejoiner), builder);
  }

  @Override
  public <KR, VR> OStream<KR, VR> map(KeyValueMapper<K, V, KeyValue<KR, VR>> mapper) {
    KeyValueMapper.TrueKeyValueMapper<K, V, KeyValue<KR, VR>> trueKeyValueMapper =
        new KeyValueMapper.TrueKeyValueMapper<>(mapper);
    return new OStreamImpl<>(ob, kstreams.map(trueKeyValueMapper), builder);
  }

  @Override
  public <VR> OStream<K, VR> mapValues(final ValueMapper<V, VR> mapper) {
    ValueMapper.TrueValueMapper<V, VR> trueValueMapper = new ValueMapper.TrueValueMapper<>(mapper);
    return new OStreamImpl<>(ob, kstreams.mapValues(trueValueMapper), builder);
  }

  @Override
  public OGroupedStream<K, V> groupByKey(final Serde<K> key, final Serde<V> value) {
    Serialized<K, V> serialized = Serialized.with(key, value);
    return new OGroupedStreamImpl<>(ob, kstreams.groupByKey(serialized), builder);
  }

  @Override
  public void foreach(ForeachAction<K, V> action) {
    ForeachAction.TrueForeachAction<K, V> trueForeachAction =
        new ForeachAction.TrueForeachAction<>(action);
    kstreams.foreach(trueForeachAction);

    Properties prop = new Properties();
    prop.setProperty(StreamsConfig.BOOTSTRAP_SERVERS, ob.bootstrapServers);
    prop.setProperty(StreamsConfig.APP_ID, ob.appId);
    prop.setProperty(StreamsConfig.CLIENT_ID, ob.appId);
    prop.setProperty(StreamsConfig.DEFAULT_KEY_SERDE, Serdes.STRING.class.getName());
    prop.setProperty(StreamsConfig.DEFAULT_VALUE_SERDE, Serdes.ROW.class.getName());
    if (ob.extractor != null) {
      prop.setProperty(StreamsConfig.TIMESTAMP_EXTRACTOR, ob.extractor.getName());
    }
    // We need to disable cache to get the aggregation result "immediately" ?...by Sam
    // Reference : https://docs.confluent.io/current/streams/developer-guide/memory-mgmt.html
    prop.setProperty(StreamsConfig.CACHE_BUFFER, "0");

    org.apache.kafka.streams.StreamsConfig config =
        new org.apache.kafka.streams.StreamsConfig(prop);

    topology = new Topology(builder, config, ob.isCleanStart, false);

    topology.start();
  }

  @Override
  public void start() {
    kstreams.to(ob.toTopic, ob.toSerdes.get());

    Properties prop = new Properties();
    prop.setProperty(StreamsConfig.BOOTSTRAP_SERVERS, ob.bootstrapServers);
    prop.setProperty(StreamsConfig.APP_ID, ob.appId);
    prop.setProperty(StreamsConfig.CLIENT_ID, ob.appId);
    prop.setProperty(StreamsConfig.DEFAULT_KEY_SERDE, Serdes.STRING.class.getName());
    prop.setProperty(StreamsConfig.DEFAULT_VALUE_SERDE, Serdes.ROW.class.getName());
    if (ob.extractor != null) {
      prop.setProperty(StreamsConfig.TIMESTAMP_EXTRACTOR, ob.extractor.getName());
    }
    // We need to disable cache to get the aggregation result "immediately" ?...by Sam
    // Reference : https://docs.confluent.io/current/streams/developer-guide/memory-mgmt.html
    prop.setProperty(StreamsConfig.CACHE_BUFFER, "0");

    org.apache.kafka.streams.StreamsConfig config =
        new org.apache.kafka.streams.StreamsConfig(prop);

    topology = new Topology(builder, config, ob.isCleanStart, false);

    topology.start();
  }

  @Override
  public void stop() {
    if (topology == null) {
      throw new RuntimeException("The StreamApp : " + ob.appId + " is not running");
    }
    topology.close();
  }

  @Override
  public String describe() {
    if (topology == null) {
      kstreams.to(ob.toTopic, ob.toSerdes.get());
      Properties prop = new Properties();
      prop.setProperty(StreamsConfig.BOOTSTRAP_SERVERS, ob.bootstrapServers);
      prop.setProperty(StreamsConfig.APP_ID, ob.appId);
      prop.setProperty(StreamsConfig.CLIENT_ID, ob.appId);

      org.apache.kafka.streams.StreamsConfig config =
          new org.apache.kafka.streams.StreamsConfig(prop);

      topology = new Topology(builder, config, ob.isCleanStart, true);
    }
    return topology.describe();
  }

  @Override
  public List<Poneglyph> getPoneglyph() {
    if (topology == null) {
      kstreams.to(ob.toTopic, ob.toSerdes.get());
      Properties prop = new Properties();
      prop.setProperty(StreamsConfig.BOOTSTRAP_SERVERS, ob.bootstrapServers);
      prop.setProperty(StreamsConfig.APP_ID, ob.appId);
      prop.setProperty(StreamsConfig.CLIENT_ID, ob.appId);

      org.apache.kafka.streams.StreamsConfig config =
          new org.apache.kafka.streams.StreamsConfig(prop);

      topology = new Topology(builder, config, ob.isCleanStart, true);
    }
    return topology.getPoneglyphs();
  }
}
