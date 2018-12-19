package com.island.ohara;

import com.island.ohara.ostreams.Consumed;
import com.island.ohara.ostreams.Predicate;
import com.island.ohara.ostreams.ValueMapper;
import java.util.Properties;

public class OStreams<K, V> {

  private org.apache.kafka.streams.StreamsBuilder builder =
      new org.apache.kafka.streams.StreamsBuilder();
  private org.apache.kafka.streams.kstream.KStream<K, V> kstreams;
  private StreamsBuilder<K, V> buildProps;

  OStreams(StreamsBuilder<K, V> b) {
    this.buildProps = b;
    this.kstreams = this.builder.stream(b.fromTopic, Consumed.with(b.keyType, b.valueType));
  }

  public OStreams<K, V> filter(Predicate<? super K, ? super V> predicate) {
    kstreams = kstreams.filter(predicate);
    return this;
  }

  public OStreams<K, V> mapValues(ValueMapper<? super V, ? extends V> mapper) {
    kstreams = kstreams.mapValues(mapper);
    return this;
  }

  public Topology construct() {
    kstreams.to(
        this.buildProps.targetTopic,
        org.apache.kafka.streams.kstream.Produced.with(
            this.buildProps.keyType, this.buildProps.valueType));
    Properties props = new Properties();
    props.setProperty(StreamsConfig.BOOTSTRAP_SERVERS, this.buildProps.bootstrapServers);
    props.setProperty(StreamsConfig.APP_ID, this.buildProps.appId);
    props.setProperty(StreamsConfig.CLIENT_ID, this.buildProps.appId);

    org.apache.kafka.streams.StreamsConfig config =
        new org.apache.kafka.streams.StreamsConfig(props);

    return new Topology(builder, config, this.buildProps.isCleanStart);
  }
}
