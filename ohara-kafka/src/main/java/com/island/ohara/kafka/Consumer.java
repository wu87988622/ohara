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

package com.island.ohara.kafka;

import com.island.ohara.common.data.Serializer;
import com.island.ohara.common.util.CommonUtils;
import com.island.ohara.common.util.Releasable;
import com.island.ohara.kafka.connector.TopicPartition;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.common.serialization.Deserializer;

/**
 * A wrap of kafka consumer.
 *
 * @param <K> key type
 * @param <V> value type
 */
public interface Consumer<K, V> extends Releasable {

  /**
   * poll the data from subscribed topics
   *
   * @param timeout waiting time
   * @return records
   */
  List<Record<K, V>> poll(Duration timeout);

  /**
   * Overloading poll method
   *
   * @param timeout waiting time
   * @param expectedSize expected size
   * @return Records
   */
  default List<Record<K, V>> poll(Duration timeout, int expectedSize) {
    return poll(timeout, expectedSize, () -> false);
  }

  default List<Record<K, V>> poll(Duration timeout, int expectedSize, Supplier<Boolean> stop) {
    return poll(timeout, expectedSize, stop, Function.identity());
  }

  default List<Record<K, V>> poll(
      Duration timeout, int expectedSize, Function<List<Record<K, V>>, List<Record<K, V>>> filter) {
    return poll(timeout, expectedSize, () -> false, filter);
  }

  /**
   * It accept another condition - expected size from records. Somethins it is helpful if you
   * already know the number from records which should be returned.
   *
   * @param timeout timeout
   * @param expectedSize the number from records should be returned
   * @param stop supply a single to stop the internal loop
   * @param filter data filter
   * @return records
   */
  default List<Record<K, V>> poll(
      Duration timeout,
      int expectedSize,
      Supplier<Boolean> stop,
      Function<List<Record<K, V>>, List<Record<K, V>>> filter) {

    List<Record<K, V>> list;
    if (expectedSize == Integer.MAX_VALUE) list = new ArrayList<>();
    else list = new ArrayList<>(expectedSize);

    long endtime = CommonUtils.current() + timeout.toMillis();
    long ramaining = endtime - CommonUtils.current();

    while (!stop.get() && list.size() < expectedSize && ramaining > 0) {
      list.addAll(filter.apply(poll(Duration.ofMillis(ramaining))));
      ramaining = endtime - CommonUtils.current();
    }
    return list;
  }

  /** @return the topic names subscribed by this consumer */
  Set<String> subscription();

  /** @return Get the set of partitions currently assigned to this consumer. */
  Set<TopicPartition> assignment();

  /**
   * Seek to the first offset for each of the given partitions.
   *
   * @param partitions setting Partition list
   */
  void seekToBeginning(Collection<TopicPartition> partitions);

  /** Seek to the first offset for all partitions */
  void seekToBeginning();

  /** break the poll right now. */
  void wakeup();

  static <Key, Value> Builder<Key, Value> builder() {
    return new Builder<>();
  }

  class Builder<Key, Value>
      implements com.island.ohara.common.pattern.Builder<Consumer<Key, Value>> {
    private Map<String, String> options = Collections.emptyMap();
    private OffsetResetStrategy fromBegin = OffsetResetStrategy.LATEST;
    private List<String> topicNames;
    private String groupId = String.format("ohara-consumer-%s", CommonUtils.uuid());
    private String connectionProps;
    private Serializer<Key> keySerializer = null;
    private Serializer<Value> valueSerializer = null;

    private Builder() {
      // do nothing
    }

    @com.island.ohara.common.annotations.Optional("default is empty")
    public Consumer.Builder<Key, Value> options(Map<String, String> options) {
      this.options = CommonUtils.requireNonEmpty(options);
      return this;
    }
    /**
     * receive all un-deleted message from subscribed topics
     *
     * @return this builder
     */
    @com.island.ohara.common.annotations.Optional("default is OffsetResetStrategy.LATEST")
    public Builder<Key, Value> offsetFromBegin() {
      this.fromBegin = OffsetResetStrategy.EARLIEST;
      return this;
    }

    /**
     * receive the messages just after the last one
     *
     * @return this builder
     */
    @com.island.ohara.common.annotations.Optional("default is OffsetResetStrategy.LATEST")
    public Builder<Key, Value> offsetAfterLatest() {
      this.fromBegin = OffsetResetStrategy.LATEST;
      return this;
    }

    /**
     * @param topicName the topic you want to subscribe
     * @return this builder
     */
    public Builder<Key, Value> topicName(String topicName) {
      this.topicNames = Collections.singletonList(Objects.requireNonNull(topicName));
      return this;
    }

    /**
     * @param topicNames the topics you want to subscribe
     * @return this builder
     */
    public Builder<Key, Value> topicNames(List<String> topicNames) {
      this.topicNames = CommonUtils.requireNonEmpty(topicNames);
      return this;
    }

    @com.island.ohara.common.annotations.Optional("default is random string")
    public Builder<Key, Value> groupId(String groupId) {
      this.groupId = Objects.requireNonNull(groupId);
      return this;
    }

    public Builder<Key, Value> connectionProps(String connectionProps) {
      this.connectionProps = CommonUtils.requireNonEmpty(connectionProps);
      return this;
    }

    public Builder<Key, Value> keySerializer(Serializer<Key> keySerializer) {
      this.keySerializer = Objects.requireNonNull(keySerializer);
      return this;
    }

    public Builder<Key, Value> valueSerializer(Serializer<Value> valueSerializer) {
      this.valueSerializer = Objects.requireNonNull(valueSerializer);
      return this;
    }

    /**
     * Used to convert byte array to ohara row. It is a private class since ohara consumer will
     * instantiate one and pass it to kafka consumer. Hence, no dynamical call will happen in kafka
     * consumer. The access exception won't be caused.
     *
     * @param serializer ohara serializer
     * @return a wrapper from kafka deserializer
     */
    private static <T> Deserializer<T> wrap(Serializer<T> serializer) {
      return new org.apache.kafka.common.serialization.Deserializer<T>() {

        @Override
        public void configure(Map<String, ?> configs, boolean isKey) {
          // do nothing
        }

        @Override
        public T deserialize(String topic, byte[] data) {
          return data == null ? null : serializer.from(data);
        }

        @Override
        public void close() {
          // do nothing
        }
      };
    }

    private void checkArguments() {
      CommonUtils.requireNonEmpty(topicNames);
      CommonUtils.requireNonEmpty(connectionProps);
      CommonUtils.requireNonEmpty(groupId);
      Objects.requireNonNull(fromBegin);
      Objects.requireNonNull(keySerializer);
      Objects.requireNonNull(valueSerializer);
    }

    @Override
    public Consumer<Key, Value> build() {
      checkArguments();

      Properties props = new Properties();
      options.forEach(props::setProperty);
      props.setProperty(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, connectionProps);
      props.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
      // kafka demand us to pass lowe case words...
      props.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, fromBegin.name().toLowerCase());

      KafkaConsumer<Key, Value> kafkaConsumer =
          new KafkaConsumer<>(props, wrap(keySerializer), wrap(valueSerializer));

      kafkaConsumer.subscribe(topicNames);

      return new Consumer<Key, Value>() {
        private ConsumerRecords<Key, Value> firstPoll = kafkaConsumer.poll(Duration.ofMillis(0));

        @Override
        public void close() {
          kafkaConsumer.close();
        }

        @Override
        public List<Record<Key, Value>> poll(Duration timeout) {

          ConsumerRecords<Key, Value> r;
          if (firstPoll == null || firstPoll.isEmpty()) r = kafkaConsumer.poll(timeout);
          else {
            r = firstPoll;
            firstPoll = null;
          }

          if (r == null || r.isEmpty()) return Collections.emptyList();
          else
            return StreamSupport.stream(
                    Spliterators.spliteratorUnknownSize(r.iterator(), Spliterator.ORDERED), false)
                .map(
                    cr ->
                        new Record<>(
                            cr.topic(),
                            cr.timestamp(),
                            TimestampType.of(cr.timestampType()),
                            cr.offset(),
                            Optional.ofNullable(cr.headers())
                                .map(
                                    headers ->
                                        StreamSupport.stream(headers.spliterator(), false)
                                            .map(header -> new Header(header.key(), header.value()))
                                            .collect(Collectors.toList()))
                                .orElse(Collections.emptyList()),
                            cr.key(),
                            cr.value()))
                .collect(Collectors.toList());
        }

        @Override
        public Set<String> subscription() {
          return Collections.unmodifiableSet(kafkaConsumer.subscription());
        }

        @Override
        public Set<TopicPartition> assignment() {
          return kafkaConsumer.assignment().stream()
              .map(x -> new TopicPartition(x.topic(), x.partition()))
              .collect(Collectors.toSet());
        }

        @Override
        public void seekToBeginning(Collection<TopicPartition> partitions) {
          kafkaConsumer.seekToBeginning(
              partitions.stream()
                  .map(
                      x -> new org.apache.kafka.common.TopicPartition(x.topicName(), x.partition()))
                  .collect(Collectors.toList()));
        }

        @Override
        public void seekToBeginning() {
          seekToBeginning(assignment());
        }

        @Override
        public void wakeup() {
          kafkaConsumer.wakeup();
        }
      };
    }
  }

  /**
   * a scala wrap from kafka's consumer record.
   *
   * @param <K> K key type
   * @param <V> V value type
   */
  class Record<K, V> {
    private final String topicName;
    private final long timestamp;
    private final TimestampType timestampType;
    private final long offset;
    private final List<Header> headers;
    private final K key;
    private final V value;

    /**
     * @param topicName topic name
     * @param timestamp time to create this record or time to append this record.
     * @param key key (nullable)
     * @param value value
     */
    private Record(
        String topicName,
        long timestamp,
        TimestampType timestampType,
        long offset,
        List<Header> headers,
        K key,
        V value) {
      this.topicName = topicName;
      this.timestamp = timestamp;
      this.timestampType = timestampType;
      this.offset = offset;
      this.headers = Collections.unmodifiableList(headers);
      this.key = key;
      this.value = value;
    }

    /**
     * Kafka topic name
     *
     * @return a topic name
     */
    public String topicName() {
      return topicName;
    }

    /**
     * The timestamp of this record.
     *
     * @return timestamp
     */
    public long timestamp() {
      return timestamp;
    }

    /**
     * The timestamp type of this record
     *
     * @return timestamp type
     */
    public TimestampType timestampType() {
      return timestampType;
    }

    /**
     * The position of this record in the corresponding Kafka partition.
     *
     * @return offset
     */
    public long offset() {
      return offset;
    }

    /**
     * The headers
     *
     * @return header list
     */
    public List<Header> headers() {
      return headers;
    }

    /**
     * The key
     *
     * @return optional key
     */
    public Optional<K> key() {
      return Optional.ofNullable(key);
    }

    /**
     * The value
     *
     * @return optional value
     */
    public Optional<V> value() {
      return Optional.ofNullable(value);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Record<?, ?> that = (Record<?, ?>) o;
      return Objects.equals(topicName, that.topicName)
          && Objects.equals(timestamp, that.timestamp)
          && Objects.equals(timestampType, that.timestampType)
          && Objects.equals(offset, that.offset)
          && CommonUtils.equals(headers, that.headers)
          && Objects.equals(key, that.key)
          && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(topicName, headers, key, value);
    }

    @Override
    public String toString() {
      return new ToStringBuilder(this)
          .append("topicName", topicName)
          .append("timestamp", timestamp)
          .append("offset", offset)
          .append("headers", headers)
          .append("key", key)
          .append("value", value)
          .toString();
    }
  }
}
