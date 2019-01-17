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
import com.island.ohara.kafka.exception.CheckedExceptionUtil;
import com.island.ohara.kafka.exception.ExceptionHandler;
import com.island.ohara.kafka.exception.OharaException;
import com.island.ohara.kafka.exception.OharaExecutionException;
import com.island.ohara.kafka.exception.OharaInterruptedException;
import com.island.ohara.kafka.exception.OharaTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
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
 * TODO: Do we really need a jave version of "broker" APIs? I don't think it is ok to allow user to
 * touch topic directly... by chia
 *
 * <p>Every method with remote call need to overload in Default Timeout
 */
public interface BrokerClient extends Releasable {

  TopicCreator topicCreator();

  boolean exist(String topicName);

  default boolean nonExist(String topicName) {
    return !exist(topicName);
  }

  /**
   * describe the topic existing in kafka. If the topic doesn't exist, exception will be thrown
   *
   * @param topicName topic name
   * @return TopicDescription
   */
  default TopicDescription topicDescription(String topicName) {
    return topicDescriptions(Collections.singletonList(topicName)).get(0);
  }

  default List<TopicDescription> topicDescriptions(List<String> topicNames) {
    return topicDescriptions()
        .stream()
        .filter(t -> topicNames.stream().anyMatch(n -> n.equals(t.name())))
        .collect(Collectors.toList());
  }

  List<TopicDescription> topicDescriptions();

  void addPartitions(String topicName, int numberOfPartitions);

  void deleteTopic(String topicName);

  String connectionProps();

  static BrokerClient of(String connectionProps) {
    Duration timeout = Duration.ofSeconds(30);
    return new BrokerClient() {

      private final AdminClient admin = AdminClient.create(toAdminProps(connectionProps));

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
      public boolean exist(String topicName) {

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
      public List<TopicDescription> topicDescriptions() {
        return topicDescriptions(topicNames());
      }

      @Override
      public List<TopicDescription> topicDescriptions(List<String> names) {
        return CheckedExceptionUtil.wrap(
            () -> {
              try {
                return admin
                    .describeTopics(names)
                    .all()
                    .get(timeout.toMillis(), TimeUnit.MILLISECONDS)
                    .entrySet()
                    .stream()
                    .map(
                        entry -> {
                          ConfigResource configKey =
                              new ConfigResource(ConfigResource.Type.TOPIC, entry.getKey());
                          try {
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
                            return new TopicDescription(
                                entry.getValue().name(),
                                entry.getValue().partitions().size(),
                                (short) entry.getValue().partitions().get(0).replicas().size(),
                                options,
                                entry.getValue().isInternal());
                          } catch (InterruptedException e) {
                            throw new OharaInterruptedException(e);
                          } catch (ExecutionException e) {
                            throw new OharaException(e.getCause());
                          }
                        })
                    .collect(Collectors.toList());
              } catch (ExecutionException e) {
                if (e.getCause() instanceof UnknownTopicOrPartitionException)
                  throw new IllegalArgumentException(
                      String.format("the %s doesn't exist", String.join(",", names)));
                throw e;
              }
            },
            handler);
      }

      @Override
      public void addPartitions(String topicName, int numberOfPartitions) {
        TopicDescription current = topicDescription(topicName);
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
      public void deleteTopic(String topicName) {
        CheckedExceptionUtil.wrap(
            () ->
                admin
                    .deleteTopics(Collections.singletonList(topicName))
                    .all()
                    .get(timeout.toMillis(), TimeUnit.MILLISECONDS),
            handler);
      }

      List<String> topicNames() {
        return CheckedExceptionUtil.wrap(
            () ->
                new ArrayList<>(
                    admin.listTopics().names().get(timeout.toMillis(), TimeUnit.MILLISECONDS)),
            handler);
      }

      @Override
      public String connectionProps() {
        return connectionProps;
      }

      /**
       * this impl will host a kafka.AdminClient so you must call the #close() to release the
       * kafka.AdminClient.
       *
       * @param brokers the kafka brokers information
       * @return a impl from BrokerClient
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
