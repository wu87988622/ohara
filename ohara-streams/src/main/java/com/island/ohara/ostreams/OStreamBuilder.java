package com.island.ohara.ostreams;

import com.island.ohara.OStream;
import com.island.ohara.TimestampExtractor;
import com.island.ohara.common.util.CommonUtil;
import java.util.Objects;

public final class OStreamBuilder<K, V> {

  String bootstrapServers = null;
  String appId = null;
  String fromTopic = null;
  Consumed fromSerdes = null;
  String toTopic = null;
  Produced toSerdes = null;
  Class<? extends TimestampExtractor> extractor = null;
  boolean isCleanStart = false;

  private Serde<K> builderKey;
  private Serde<V> builderValue;

  public OStreamBuilder(Serde<K> key, Serde<V> value) {
    this.builderKey = key;
    this.builderValue = value;
  }

  public OStreamBuilder<K, V> bootstrapServers(String bootstrapServers) {
    this.bootstrapServers = bootstrapServers;
    return this;
  }

  public OStreamBuilder<K, V> appid(String appId) {
    this.appId = appId;
    return this;
  }

  /**
   * set the topic consume from. note the default {@code <key, value>} is {@code <Serdes.String,
   * Serdes.Row>}
   *
   * @param fromTopic the topic name
   */
  public OStreamBuilder<K, V> fromTopic(String fromTopic) {
    this.fromTopic = fromTopic;
    this.fromSerdes = new Consumed(builderKey, builderValue);
    return this;
  }

  /**
   * you can change the consuming serializer/de-serializer. however you should check the {@code
   * <key, value>} by your own
   */
  public OStreamBuilder<?, ?> fromTopicWith(String fromTopic, Serde key, Serde value) {
    this.builderKey = key;
    this.builderValue = value;
    this.fromTopic = fromTopic;
    this.fromSerdes = new Consumed(key, value);
    return this;
  }

  /**
   * set the topic produce to. note the default {@code <key, value>} is {@code <Serdes.String,
   * Serdes.Row>}
   *
   * @param toTopic the topic name
   */
  public OStreamBuilder<K, V> toTopic(String toTopic) {
    return toTopicWith(toTopic, builderKey, builderValue);
  }

  public OStreamBuilder<K, V> toTopicWith(String toTopic, Serde key, Serde value) {
    this.toTopic = toTopic;
    this.toSerdes = new Produced(key, value);
    return this;
  }

  public OStreamBuilder<K, V> cleanStart() {
    this.isCleanStart = true;
    return this;
  }

  public OStreamBuilder<K, V> timestampExactor(Class<? extends TimestampExtractor> extractor) {
    this.extractor = extractor;
    return this;
  }

  public OStream<K, V> build() {
    // Validation
    Objects.requireNonNull(this.bootstrapServers, "bootstrapServers should not be null");
    Objects.requireNonNull(this.fromTopic, "fromTopic should not be null");
    Objects.requireNonNull(this.toTopic, "targetTopic should not be null");

    // Default
    if (this.appId == null) {
      this.appId = CommonUtil.uuid() + "-streamApp";
    }

    return new OStreamImpl<>(this);
  }
}
