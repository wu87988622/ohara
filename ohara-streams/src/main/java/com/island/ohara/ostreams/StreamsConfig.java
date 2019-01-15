package com.island.ohara.ostreams;

import org.apache.kafka.clients.consumer.ConsumerConfig;

public class StreamsConfig {
  static final String BOOTSTRAP_SERVERS =
      org.apache.kafka.streams.StreamsConfig.BOOTSTRAP_SERVERS_CONFIG;
  static final String APP_ID = org.apache.kafka.streams.StreamsConfig.APPLICATION_ID_CONFIG;
  static final String CLIENT_ID = org.apache.kafka.streams.StreamsConfig.CLIENT_ID_CONFIG;
  static final String STATE_DIR = org.apache.kafka.streams.StreamsConfig.STATE_DIR_CONFIG;
  static final String DEFAULT_KEY_SERDE =
      org.apache.kafka.streams.StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG;
  static final String DEFAULT_VALUE_SERDE =
      org.apache.kafka.streams.StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG;
  static final String TIMESTAMP_EXTRACTOR =
      org.apache.kafka.streams.StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG;
  static final String AUTO_OFFSET = ConsumerConfig.AUTO_OFFSET_RESET_CONFIG;
  static final String CACHE_BUFFER =
      org.apache.kafka.streams.StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG;
  static final String THREADS = org.apache.kafka.streams.StreamsConfig.NUM_STREAM_THREADS_CONFIG;
  static final String GUARANTEE =
      org.apache.kafka.streams.StreamsConfig.PROCESSING_GUARANTEE_CONFIG;
}
