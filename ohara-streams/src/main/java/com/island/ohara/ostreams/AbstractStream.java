package com.island.ohara.ostreams;

import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KGroupedStream;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;

abstract class AbstractStream<K, V> {

  KTable<K, V> ktable;
  KStream<K, V> kstreams;
  KGroupedStream<K, V> kgroupstream;
  OStreamBuilder ob;
  StreamsBuilder builder;

  AbstractStream(final OStreamBuilder ob) {
    StreamsBuilder newBuilder = new StreamsBuilder();
    this.kstreams = newBuilder.stream(ob.fromTopic, ob.fromSerdes.get());
    this.ob = ob;
    this.builder = newBuilder;
  }

  AbstractStream(final OStreamBuilder ob, final KStream<K, V> kstreams, StreamsBuilder builder) {
    this.ob = ob;
    this.kstreams = kstreams;
    this.builder = builder;
  }

  AbstractStream(
      final OStreamBuilder ob, final KGroupedStream<K, V> kgroupstream, StreamsBuilder builder) {
    this.ob = ob;
    this.kgroupstream = kgroupstream;
    this.builder = builder;
  }

  AbstractStream(final OStreamBuilder ob, final KTable<K, V> ktable, StreamsBuilder builder) {
    this.ob = ob;
    this.ktable = ktable;
    this.builder = builder;
  }
}
