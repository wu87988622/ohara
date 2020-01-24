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

import com.island.ohara.common.data.Pair;
import com.island.ohara.common.setting.TopicKey;
import com.island.ohara.common.util.CommonUtils;
import com.island.ohara.common.util.Releasable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;

/**
 * a helper methods used by configurator. It provide many helper method to operate kafka cluster.
 * TODO: Do we really need a jave version of "broker" APIs? I don't think it is ok to allow user to
 * touch topic directly... by chia
 *
 * <p>Every method with remote call need to overload in Default Timeout
 */
public interface TopicAdmin extends Releasable {

  /**
   * start a progress to create topic
   *
   * @return creator
   */
  TopicCreator topicCreator();

  /**
   * @param name topic name
   * @return true if topic exists. Otherwise, false
   */
  CompletionStage<Boolean> exist(String name);

  /**
   * @param key topic key
   * @return true if topic exists. Otherwise, false
   */
  default CompletionStage<Boolean> exist(TopicKey key) {
    return exist(key.topicNameOnKafka());
  }

  /**
   * describe the topic existing in kafka. If the topic doesn't exist, exception will be thrown
   *
   * @param key topic key
   * @return TopicDescription
   */
  default CompletionStage<TopicDescription> topicDescription(TopicKey key) {
    return topicDescription(key.topicNameOnKafka());
  }

  default CompletionStage<TopicDescription> topicDescription(String name) {
    return topicDescriptions(Collections.singleton(name))
        .thenApply(
            tds -> {
              if (tds.isEmpty()) throw new NoSuchElementException(name + " does not exist");
              else return tds.get(0);
            });
  }

  default CompletionStage<List<TopicDescription>> topicDescriptions(Set<String> topicNames) {
    return topicDescriptions()
        .thenApply(
            tds ->
                tds.stream()
                    .filter(td -> topicNames.stream().anyMatch(n -> n.equals(td.name())))
                    .collect(Collectors.toList()));
  }

  /**
   * list all topics details from kafka
   *
   * @return topic details
   */
  CompletionStage<List<TopicDescription>> topicDescriptions();

  /**
   * create new partitions for specified topic
   *
   * @param name topic name
   * @param numberOfPartitions number of new partitions
   * @return a async callback is tracing the result
   */
  CompletionStage<Void> createPartitions(String name, int numberOfPartitions);

  /**
   * create new partitions for specified topic
   *
   * @param key topic key
   * @param numberOfPartitions number of new partitions
   * @return a async callback is tracing the result
   */
  default CompletionStage<Void> createPartitions(TopicKey key, int numberOfPartitions) {
    return createPartitions(key.topicNameOnKafka(), numberOfPartitions);
  }

  /**
   * remove topic.
   *
   * @param key topic key
   * @return a async callback is tracing the result. It carries the "true" if it does remove a
   *     topic. otherwise, false
   */
  default CompletionStage<Boolean> deleteTopic(TopicKey key) {
    return deleteTopic(key.topicNameOnKafka());
  }

  /**
   * remove topic.
   *
   * @param name topic encoded name
   * @return a async callback is tracing the result. It carries the "true" if it does remove a
   *     topic. otherwise, false
   */
  CompletionStage<Boolean> deleteTopic(String name);

  /** @return Connection information. form: host:port,host:port */
  String connectionProps();

  /**
   * list all active brokers' ports. This is different to {@link #connectionProps()} since this
   * method will fetch the "really" active brokers from cluster. {@link #connectionProps()} may
   * include dead or nonexistent broker nodes.
   *
   * @return active brokers' ports
   */
  CompletionStage<Map<String, Integer>> brokerPorts();

  boolean closed();

  static TopicAdmin of(String connectionProps) {
    return new TopicAdmin() {

      private final AdminClient admin = AdminClient.create(toAdminProps(connectionProps));

      private final AtomicBoolean closed = new AtomicBoolean(false);

      private Map<String, List<PartitionInfo>> partitionInfos() {
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, connectionProps);
        properties.setProperty(ConsumerConfig.CLIENT_ID_CONFIG, CommonUtils.randomString());
        try (KafkaConsumer<byte[], byte[]> consumer =
            new KafkaConsumer<>(
                properties, new ByteArrayDeserializer(), new ByteArrayDeserializer())) {
          return consumer.listTopics().entrySet().stream()
              .map(
                  entry -> {
                    List<TopicPartition> tps =
                        entry.getValue().stream()
                            .map(p -> new TopicPartition(p.topic(), p.partition()))
                            .collect(Collectors.toList());
                    Map<TopicPartition, Long> beginningOffsets = consumer.beginningOffsets(tps);
                    Map<TopicPartition, Long> endOffsets = consumer.endOffsets(tps);
                    List<PartitionInfo> ps =
                        entry.getValue().stream()
                            .map(
                                p -> {
                                  TopicPartition tp = new TopicPartition(p.topic(), p.partition());
                                  return new PartitionInfo(
                                      p.partition(),
                                      PartitionNode.of(p.leader()),
                                      p.replicas() == null
                                          ? Collections.emptyList()
                                          : Arrays.stream(p.replicas())
                                              .map(PartitionNode::of)
                                              .collect(Collectors.toList()),
                                      p.inSyncReplicas() == null
                                          ? Collections.emptyList()
                                          : Arrays.stream(p.inSyncReplicas())
                                              .map(PartitionNode::of)
                                              .collect(Collectors.toList()),
                                      beginningOffsets.getOrDefault(tp, -1L),
                                      endOffsets.getOrDefault(tp, -1L));
                                })
                            .collect(Collectors.toList());
                    return Pair.of(entry.getKey(), ps);
                  })
              .collect(Collectors.toMap(Pair::left, Pair::right));
        }
      }

      @Override
      public TopicCreator topicCreator() {
        return new TopicCreator() {
          @Override
          protected CompletionStage<Void> doCreate(
              int numberOfPartitions,
              short numberOfReplications,
              Map<String, String> options,
              String name) {
            CompletableFuture<Void> f = new CompletableFuture<>();
            admin
                .createTopics(
                    Collections.singletonList(
                        new NewTopic(name, numberOfPartitions, numberOfReplications)
                            .configs(options)))
                .values()
                .get(name)
                .whenComplete(
                    (v, exception) -> {
                      if (exception != null) f.completeExceptionally(exception);
                      else f.complete(null);
                    });
            return f;
          }
        };
      }

      @Override
      public CompletionStage<Boolean> exist(String name) {
        return topicNames().thenApply(tps -> tps.contains(name));
      }

      private CompletionStage<Set<String>> topicNames() {
        CompletableFuture<Set<String>> f = new CompletableFuture<>();
        admin
            .listTopics()
            .names()
            .whenComplete(
                (tps, exception) -> {
                  if (exception != null) f.completeExceptionally(exception);
                  else f.complete(tps);
                });
        return f;
      }

      private CompletionStage<Map<String, List<TopicOption>>> options(Set<String> topicNames) {
        return topicNames()
            .thenApply(
                names ->
                    names.stream()
                        .map(name -> new ConfigResource(ConfigResource.Type.TOPIC, name))
                        .collect(Collectors.toList()))
            .thenCompose(
                rs -> {
                  CompletableFuture<Map<String, List<TopicOption>>> f = new CompletableFuture<>();
                  admin
                      .describeConfigs(rs)
                      .all()
                      .whenComplete(
                          (configs, exception) -> {
                            if (exception != null) f.completeExceptionally(exception);
                            else
                              f.complete(
                                  configs.entrySet().stream()
                                      .map(
                                          entry ->
                                              Pair.of(
                                                  entry.getKey().name(),
                                                  entry.getValue().entries().stream()
                                                      .map(
                                                          o ->
                                                              new TopicOption(
                                                                  o.name(),
                                                                  o.value(),
                                                                  o.isDefault(),
                                                                  o.isSensitive(),
                                                                  o.isReadOnly()))
                                                      .collect(Collectors.toList())))
                                      .collect(Collectors.toMap(Pair::left, Pair::right)));
                          });
                  return f;
                });
      }

      @Override
      public CompletionStage<List<TopicDescription>> topicDescriptions() {
        return topicNames()
            .thenCompose(this::options)
            .thenApply(
                nameAndOpts -> {
                  Map<String, List<PartitionInfo>> partitionInfos = partitionInfos();
                  return nameAndOpts.entrySet().stream()
                      .map(
                          entry -> {
                            List<PartitionInfo> infos =
                                partitionInfos.getOrDefault(
                                    entry.getKey(), Collections.emptyList());
                            return new TopicDescription(entry.getKey(), infos, entry.getValue());
                          })
                      .collect(Collectors.toList());
                });
      }

      @Override
      public CompletionStage<Void> createPartitions(String name, int numberOfPartitions) {
        return topicDescription(name)
            .thenCompose(
                current -> {
                  if (current.numberOfPartitions() > numberOfPartitions)
                    throw new IllegalArgumentException(
                        "Reducing the number from partitions is disallowed. current:"
                            + current.numberOfPartitions()
                            + ", expected:"
                            + numberOfPartitions);
                  else if (current.numberOfPartitions() == numberOfPartitions)
                    return CompletableFuture.completedFuture(null);
                  else {
                    CompletableFuture<Void> f = new CompletableFuture<>();
                    admin
                        .createPartitions(
                            Collections.singletonMap(
                                name, NewPartitions.increaseTo(numberOfPartitions)))
                        .values()
                        .get(name)
                        .whenComplete(
                            (v, exception) -> {
                              if (exception != null) f.completeExceptionally(exception);
                              else f.complete(null);
                            });
                    return f;
                  }
                });
      }

      @Override
      public CompletionStage<Boolean> deleteTopic(String topicName) {
        CompletableFuture<Boolean> f = new CompletableFuture<>();
        topicNames()
            .thenApply(names -> names.stream().anyMatch(name -> name.equals(topicName)))
            .whenComplete(
                (existent, exception) -> {
                  if (exception != null) f.completeExceptionally(exception);
                  else f.complete(existent);
                });
        return f.thenCompose(
            existent -> {
              if (existent) {
                CompletableFuture<Boolean> f2 = new CompletableFuture<>();
                admin
                    .deleteTopics(Collections.singletonList(topicName))
                    .values()
                    .get(topicName)
                    .whenComplete(
                        (v, exception) -> {
                          if (exception != null) f2.completeExceptionally(exception);
                          else f2.complete(true);
                        });
                return f2;
              } else return CompletableFuture.completedFuture(false);
            });
      }

      @Override
      public String connectionProps() {
        return connectionProps;
      }

      @Override
      public CompletionStage<Map<String, Integer>> brokerPorts() {
        CompletableFuture<Map<String, Integer>> f = new CompletableFuture<>();
        admin
            .describeCluster()
            .nodes()
            .whenComplete(
                (nodes, exception) -> {
                  if (exception != null) f.completeExceptionally(exception);
                  else f.complete(nodes.stream().collect(Collectors.toMap(Node::host, Node::port)));
                });
        return f;
      }

      /**
       * this impl will host a kafka.AdminClient so you must call the #close() to release the
       * kafka.AdminClient.
       *
       * @param brokers the kafka brokers information
       * @return the props for TopicAdmin
       */
      private Properties toAdminProps(String brokers) {
        Properties adminProps = new Properties();
        adminProps.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokers);
        return adminProps;
      }

      @Override
      public void close() {
        if (closed.compareAndSet(false, true)) {
          admin.close();
        }
      }

      @Override
      public boolean closed() {
        return closed.get();
      }
    };
  }
}
