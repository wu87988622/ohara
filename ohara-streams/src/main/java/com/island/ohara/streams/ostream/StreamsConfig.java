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
