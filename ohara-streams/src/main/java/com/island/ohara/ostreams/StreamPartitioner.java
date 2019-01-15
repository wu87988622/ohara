package com.island.ohara.ostreams;

public interface StreamPartitioner<K, V> {

  Integer partition(K key, V value, int numPartitions);

  class TrueStreamPartitioner<K, V>
      implements org.apache.kafka.streams.processor.StreamPartitioner<K, V> {

    private final StreamPartitioner<K, V> trueStreamPartitioner;

    TrueStreamPartitioner(StreamPartitioner<K, V> streamPartitioner) {
      this.trueStreamPartitioner = streamPartitioner;
    }

    @Override
    public Integer partition(K key, V value, int numPartitions) {
      return this.trueStreamPartitioner.partition(key, value, numPartitions);
    }
  }
}
