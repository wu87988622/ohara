package com.island.ohara.kafka;

import com.island.ohara.common.data.Serializer;
import com.island.ohara.common.util.CommonUtil;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;

/**
 * a simple scala wrap from kafka consumer.
 *
 * @tparam K key type
 * @tparam V value type
 */
public final class ConsumerBuilder {

  private OffsetResetStrategy fromBegin = OffsetResetStrategy.LATEST;
  private List<String> topicNames;
  private String groupId = String.format("ohara-consumer- %s", CommonUtil.uuid());
  private String brokers;

  /**
   * receive all un-deleted message from subscribed topics
   *
   * @return this builder
   */
  public ConsumerBuilder offsetFromBegin() {
    this.fromBegin = OffsetResetStrategy.EARLIEST;
    return this;
  }

  /**
   * receive the messages just after the last one
   *
   * @return this builder
   */
  public ConsumerBuilder offsetAfterLatest() {
    this.fromBegin = OffsetResetStrategy.LATEST;
    return this;
  }

  /**
   * @param topicName the topic you want to subscribe
   * @return this builder
   */
  public ConsumerBuilder topicName(String topicName) {
    this.topicNames = Collections.singletonList(topicName);
    return this;
  }

  /**
   * @param topicNames the topics you want to subscribe
   * @return this builder
   */
  public ConsumerBuilder topicNames(List<String> topicNames) {
    this.topicNames = topicNames;
    return this;
  }

  public ConsumerBuilder groupId(String groupId) {
    this.groupId = groupId;
    return this;
  }

  public ConsumerBuilder brokers(String brokers) {
    this.brokers = brokers;
    return this;
  }

  public <K, V> Consumer<K, V> build(Serializer<K> keySerializer, Serializer<V> valueSerializer) {
    Objects.requireNonNull(topicNames);
    if (topicNames.isEmpty()) throw new IllegalArgumentException("Topics list is empty");
    Objects.requireNonNull(groupId);
    Objects.requireNonNull(brokers);

    Properties consumerConfig = new Properties();

    consumerConfig.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokers);
    consumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    // kafka demand us to pass lowe case words...
    consumerConfig.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, fromBegin.name().toLowerCase());

    KafkaConsumer<K, V> kafkaConsumer =
        new KafkaConsumer<>(
            consumerConfig,
            KafkaUtil.wrapDeserializer(keySerializer),
            KafkaUtil.wrapDeserializer(valueSerializer));

    kafkaConsumer.subscribe(topicNames);

    return new Consumer<K, V>() {
      private ConsumerRecords<K, V> firstPoll = kafkaConsumer.poll(0);

      @Override
      public void close() {
        kafkaConsumer.close();
      }

      @Override
      public List<ConsumerRecord<K, V>> poll(Duration timeout) {

        ConsumerRecords<K, V> r;
        if (firstPoll == null || firstPoll.isEmpty()) r = kafkaConsumer.poll(timeout.toMillis());
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
                      new ConsumerRecord<>(
                          cr.topic(),
                          Optional.ofNullable(cr.headers())
                              .map(
                                  headers ->
                                      StreamSupport.stream(headers.spliterator(), false)
                                          .map(header -> new Header(header.key(), header.value()))
                                          .collect(Collectors.toList()))
                              .orElseGet(() -> Collections.emptyList()),
                          cr.key(),
                          cr.value()))
              .collect(Collectors.toList());
      }

      @Override
      public Set<String> subscription() {
        return Collections.unmodifiableSet(kafkaConsumer.subscription());
      }

      @Override
      public void wakeup() {
        kafkaConsumer.wakeup();
      }
    };
  }
}
