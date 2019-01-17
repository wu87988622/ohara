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

import com.island.ohara.common.util.Releasable;
import com.island.ohara.kafka.exception.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;

/**
 * a helper methods used by configurator. It provide many helper method to operate kafka cluster.
 *
 * <p>Every method with remote call need to overload in Default Timeout
 */
public interface KafkaClient extends Releasable {
  Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

  TopicCreator topicCreator();

  boolean exist(String topicName, Duration timeout);

  default boolean exist(String topicName) {
    return exist(topicName, DEFAULT_TIMEOUT);
  }

  default boolean nonExist(String topicName, Duration timeout) {
    return !exist(topicName, timeout);
  }

  default boolean nonExist(String topicName) {
    return !exist(topicName, DEFAULT_TIMEOUT);
  }

  /**
   * describe the topic existing in kafka. If the topic doesn't exist, exception will be thrown
   *
   * @param topicName topic name
   * @param timeout timeout
   * @return TopicDescription
   */
  TopicDescription topicDescription(String topicName, Duration timeout);

  default TopicDescription topicDescription(String topicName) {
    return topicDescription(topicName, DEFAULT_TIMEOUT);
  }

  void addPartitions(String topicName, int numberOfPartitions, Duration timeout);

  default void addPartitions(String topicName, int numberOfPartitions) {
    addPartitions(topicName, numberOfPartitions, DEFAULT_TIMEOUT);
  }

  void deleteTopic(String topicName, Duration timeout);

  default void deleteTopic(String topicName) {
    deleteTopic(topicName, DEFAULT_TIMEOUT);
  }

  List<String> listTopics(Duration timeout);

  default List<String> listTopics() {
    return listTopics(DEFAULT_TIMEOUT);
  }

  String brokers();

  ConsumerBuilder consumerBuilder();

  static KafkaClient of(String outerbrokers) {
    return new KafkaClient() {

      private final String brokers = outerbrokers;

      private final AdminClient admin = AdminClient.create(toAdminProps(outerbrokers));

      private final ExceptionHandler handler =
          ExceptionHandler.creator()
              .add(ExecutionException.class, (e) -> new OharaExecutionException(e.getCause()))
              .add(InterruptedException.class, OharaInterruptedException::new)
              .add(TimeoutException.class, OharaTimeoutException::new)
              .create();

      @Override
      public TopicCreator topicCreator() {
        return new TopicCreator() {

          @Override
          public void create(String name) {
            CheckedExceptionUtil.wrap(
                () ->
                    admin
                        .createTopics(
                            Collections.singletonList(
                                new NewTopic(name, numberOfPartitions, numberOfReplications)
                                    .configs(options)))
                        .values()
                        .get(name)
                        .get(timeout.toMillis(), TimeUnit.MILLISECONDS),
                handler);
          }
        };
      }

      @Override
      public boolean exist(String topicName, Duration timeout) {

        return CheckedExceptionUtil.wrap(
            () ->
                admin
                    .listTopics()
                    .names()
                    .thenApply(
                        new KafkaFuture.Function<Set<String>, Boolean>() {
                          @Override
                          public Boolean apply(Set<String> strings) {
                            return strings.contains(topicName);
                          }
                        })
                    .get(timeout.toMillis(), TimeUnit.MILLISECONDS),
            handler);
      }

      @Override
      public TopicDescription topicDescription(String topicName, Duration timeout) {
        return CheckedExceptionUtil.wrap(
            () -> {
              try {
                ConfigResource configKey = new ConfigResource(ConfigResource.Type.TOPIC, topicName);
                List<TopicOption> options =
                    admin
                        .describeConfigs(Collections.singletonList(configKey))
                        .values()
                        .get(configKey)
                        .get()
                        .entries()
                        .stream()
                        .map(
                            o ->
                                new TopicOption(
                                    o.name(),
                                    o.value(),
                                    o.isDefault(),
                                    o.isSensitive(),
                                    o.isReadOnly()))
                        .collect(Collectors.toList());

                org.apache.kafka.clients.admin.TopicDescription topicPartitionInfo =
                    Optional.ofNullable(
                            admin
                                .describeTopics(Collections.singletonList(topicName))
                                .values()
                                .get(topicName))
                        .orElseThrow(
                            () ->
                                new IllegalArgumentException(
                                    String.format("the topic:%s isn't existed", topicName)))
                        .get(timeout.toMillis(), TimeUnit.MILLISECONDS);

                return new TopicDescription(
                    topicPartitionInfo.name(),
                    topicPartitionInfo.partitions().size(),
                    (short) topicPartitionInfo.partitions().get(0).replicas().size(),
                    options);

              } catch (ExecutionException e) {
                if (e.getCause() instanceof UnknownTopicOrPartitionException)
                  throw new IllegalArgumentException(
                      String.format("the %s doesn't exist", topicName));
                throw e;
              }
            },
            handler);
      }

      @Override
      public void addPartitions(String topicName, int numberOfPartitions, Duration timeout) {
        TopicDescription current = topicDescription(topicName, timeout);
        if (current.numberOfPartitions() > numberOfPartitions)
          throw new IllegalArgumentException("Reducing the number from partitions is disallowed");
        if (current.numberOfPartitions() < numberOfPartitions) {
          Map<String, NewPartitions> map = new HashMap<>();
          map.put(topicName, NewPartitions.increaseTo(numberOfPartitions));

          CheckedExceptionUtil.wrap(
              () ->
                  admin.createPartitions(map).all().get(timeout.toMillis(), TimeUnit.MILLISECONDS),
              handler);
        }
      }

      @Override
      public void deleteTopic(String topicName, Duration timeout) {
        CheckedExceptionUtil.wrap(
            () ->
                admin
                    .deleteTopics(Collections.singletonList(topicName))
                    .all()
                    .get(timeout.toMillis(), TimeUnit.MILLISECONDS),
            handler);
      }

      @Override
      public List<String> listTopics(Duration timeout) {
        return CheckedExceptionUtil.wrap(
            () ->
                new ArrayList<>(
                    admin.listTopics().names().get(timeout.toMillis(), TimeUnit.MILLISECONDS)),
            handler);
      }

      @Override
      public String brokers() {
        return brokers;
      }

      @Override
      public ConsumerBuilder consumerBuilder() {
        return new ConsumerBuilder().brokers(brokers);
      }

      /**
       * this impl will host a kafka.AdminClient so you must call the #close() to release the
       * kafka.AdminClient.
       *
       * @param brokers the kafka brokers information
       * @return a impl from KafkaClient
       */
      private Properties toAdminProps(String brokers) {
        Properties adminProps = new Properties();
        adminProps.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokers);
        return adminProps;
      }

      @Override
      public void close() {
        admin.close();
      }
    };
  }
}
