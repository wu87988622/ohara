package com.island.ohara;

public class StreamsConfig {
  static final String BOOTSTRAP_SERVERS =
      org.apache.kafka.streams.StreamsConfig.BOOTSTRAP_SERVERS_CONFIG;
  static final String APP_ID = org.apache.kafka.streams.StreamsConfig.APPLICATION_ID_CONFIG;
  static final String CLIENT_ID = org.apache.kafka.streams.StreamsConfig.CLIENT_ID_CONFIG;
  static final String STATE_DIR = org.apache.kafka.streams.StreamsConfig.STATE_DIR_CONFIG;
}
