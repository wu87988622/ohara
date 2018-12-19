package com.island.ohara;

import com.island.ohara.common.util.CommonUtil;
import com.island.ohara.ostreams.Serde;
import java.util.Objects;

public final class StreamsBuilder<K, V> {

  final Serde<K> keyType;
  final Serde<V> valueType;

  String bootstrapServers;
  String appId;
  String fromTopic = "";
  String targetTopic = "";
  boolean isCleanStart = false;

  private StreamsBuilder(Serde<K> keyType, Serde<V> valueType) {
    this.keyType = keyType;
    this.valueType = valueType;
  }

  public static <S, U> StreamsBuilder<S, U> of(Serde<S> keyType, Serde<U> valueType) {
    return new StreamsBuilder<>(keyType, valueType);
  }

  public StreamsBuilder<K, V> bootstrapServers(String bootstrapServers) {
    this.bootstrapServers = bootstrapServers;
    return this;
  }

  public StreamsBuilder<K, V> appid(String appId) {
    this.appId = appId;
    return this;
  }

  public StreamsBuilder<K, V> fromTopic(String topic) {
    this.fromTopic = topic;
    return this;
  }

  public StreamsBuilder<K, V> toTopic(String topic) {
    this.targetTopic = topic;
    return this;
  }

  public StreamsBuilder<K, V> cleanStart() {
    this.isCleanStart = true;
    return this;
  }

  public OStreams<K, V> build() {
    // Validation
    Objects.requireNonNull(this.bootstrapServers, "bootstrapServers should not be null");
    Objects.requireNonNull(this.fromTopic, "fromTopic should not be null");
    Objects.requireNonNull(this.targetTopic, "targetTopic should not be null");

    // Default
    if (this.appId == null) {
      this.appId = CommonUtil.uuid() + "-streamApp";
    }

    return new OStreams<>(this);
  }
}
