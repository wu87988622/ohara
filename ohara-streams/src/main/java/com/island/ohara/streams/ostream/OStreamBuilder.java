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

import com.island.ohara.common.data.Row;
import com.island.ohara.streams.OStream;

/**
 * This class is responsible for managing all the properties that will use in {@code OStream}. Use
 * this class to construct {@code OStream} only.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class OStreamBuilder<K, V> {

  private String bootstrapServers = null;
  private String appId = null;
  private String fromTopic = null;
  private Consumed fromSerde = null;
  private String toTopic = null;
  private Produced toSerde = null;
  private Class<? extends TimestampExtractor> extractor = null;
  private boolean cleanStart = false;
  private boolean exactlyOnce = false;

  // for inner use
  private final Serde<K> builderKeySerde;
  private final Serde<V> builderValueSerde;

  public OStreamBuilder(Serde<K> key, Serde<V> value) {
    this.builderKeySerde = key;
    this.builderValueSerde = value;
  }

  private OStreamBuilder(OStreamBuilder builder) {
    this.bootstrapServers = builder.bootstrapServers;
    this.appId = builder.appId;
    this.fromTopic = builder.fromTopic;
    this.fromSerde = builder.fromSerde;
    this.toTopic = builder.toTopic;
    this.toSerde = builder.toSerde;
    this.extractor = builder.extractor;
    this.cleanStart = builder.cleanStart;
    this.builderKeySerde = builder.builderKeySerde;
    this.builderValueSerde = builder.builderValueSerde;
    this.exactlyOnce = builder.exactlyOnce;
  }

  /**
   * @param bootstrapServers broker list
   * @return this builder
   */
  OStreamBuilder<K, V> bootstrapServers(String bootstrapServers) {
    this.bootstrapServers = bootstrapServers;
    return this;
  }

  /**
   * @param appId the application.id you want to group streamApp for
   * @return this builder
   */
  OStreamBuilder<K, V> appid(String appId) {
    this.appId = appId;
    return this;
  }

  /**
   * set the topic consumed from. note the default {@code <key, value>} is {@code <Serdes.String,
   * Serdes.StreamRow>}
   *
   * @param fromTopic the topic name
   * @return this builder
   */
  OStreamBuilder<K, V> fromTopic(String fromTopic) {
    this.fromTopic = fromTopic;
    this.fromSerde = new Consumed<>(builderKeySerde, builderValueSerde);
    return this;
  }

  /**
   * set the topic consumed from by providing the serializer/de-serializer.
   *
   * @param fromTopic the topic name
   * @param key the serialize type for topic key
   * @param value the serialize type for topic value
   * @param <S> the source topic key type
   * @param <U> the source topic value type
   * @return this builder
   */
  <S, U> OStreamBuilder<S, U> fromTopicWith(String fromTopic, Serde<S> key, Serde<U> value) {
    this.fromTopic = fromTopic;
    this.fromSerde = new Consumed<>(key, value);
    return new OStreamBuilder<>(this);
  }

  /**
   * set the topic produced to. note the default {@code <key, value>} is {@code <Serdes.String,
   * Serdes.StreamRow>}
   *
   * @param toTopic the topic name
   * @return this builder
   */
  OStreamBuilder<K, V> toTopic(String toTopic) {
    return toTopicWith(toTopic, builderKeySerde, builderValueSerde);
  }

  /**
   * set the topic produced from by providing the serializer/de-serializer.
   *
   * @param toTopic the topic name
   * @param key the serialize type for topic key
   * @param value the serialize type for topic value
   * @param <S> the target topic key type
   * @param <U> the target topic value type
   * @return this builder
   */
  <S, U> OStreamBuilder<K, V> toTopicWith(String toTopic, Serde<S> key, Serde<U> value) {
    this.toTopic = toTopic;
    this.toSerde = new Produced<>(key, value);
    return this;
  }

  /**
   * enable exactly once
   *
   * @return this builder
   */
  public OStreamBuilder<K, V> enableExactlyOnce() {
    this.exactlyOnce = true;
    return this;
  }

  /**
   * control this stream application should clean all state data before start.
   *
   * @return this builder
   */
  public OStreamBuilder<K, V> cleanStart() {
    this.cleanStart = true;
    return this;
  }

  /**
   * define timestamp of fromTopic records.
   *
   * @param extractor class extends {@code TimestampExtractor}
   * @return this builder
   */
  public OStreamBuilder<K, V> timestampExtractor(Class<? extends TimestampExtractor> extractor) {
    this.extractor = extractor;
    return this;
  }

  // This is for testing
  OStream<Row> build() {
    return new OStreamImpl(this);
  }

  // Getters
  String getBootstrapServers() {
    return bootstrapServers;
  }

  String getAppId() {
    return appId;
  }

  String getFromTopic() {
    return fromTopic;
  }

  Consumed getFromSerde() {
    return fromSerde;
  }

  String getToTopic() {
    return toTopic;
  }

  Produced getToSerde() {
    return toSerde;
  }

  Class<? extends TimestampExtractor> getExtractor() {
    return extractor;
  }

  boolean isCleanStart() {
    return cleanStart;
  }

  boolean getExactlyOnce() {
    return exactlyOnce;
  }
}
