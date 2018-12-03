package com.island.ohara.kafka;

import com.island.ohara.kafka.exception.CheckedExceptionUtil;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
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
public interface KafkaClient extends AutoCloseable {
  static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

  TopicCreator topicCreator();

  boolean exist(String topicName, Duration timeout);

  default boolean exist(String topicName) {
    return exist(topicName, DEFAULT_TIMEOUT);
  };

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
  };

  void addPartitions(String topicName, int numberOfPartitions, Duration timeout);

  default void addPartitions(String topicName, int numberOfPartitions) {
    addPartitions(topicName, numberOfPartitions, DEFAULT_TIMEOUT);
  }

  void deleteTopic(String topicName, Duration timeout);

  default void deleteTopic(String topicName) {
    deleteTopic(topicName, DEFAULT_TIMEOUT);
  };

  List<String> listTopics(Duration timeout);

  default List<String> listTopics() {
    return listTopics(DEFAULT_TIMEOUT);
  };

  String brokers();

  ConsumerBuilder consumerBuilder();

  void close();

  static KafkaClient of(String outerbrokers) {
    return new KafkaClient() {

      private String brokers = outerbrokers;

      private Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
      private AdminClient admin = AdminClient.create(toAdminProps(outerbrokers));

      @Override
      public TopicCreator topicCreator() {
        return new TopicCreator() {

          @Override
          protected void doCreate(String name) {
            CheckedExceptionUtil.wrap(
                () ->
                    admin
                        .createTopics(
                            Arrays.asList(
                                new NewTopic(name, numberOfPartitions(), numberOfReplications())
                                    .configs(options())))
                        .values()
                        .get(name)
                        .get(timeout().toMillis(), TimeUnit.MILLISECONDS));
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
                    .get(timeout.toMillis(), TimeUnit.MILLISECONDS));
      }

      @Override
      public TopicDescription topicDescription(String topicName, Duration timeout) {
        return CheckedExceptionUtil.wrap(
            () -> {
              try {
                ConfigResource configKey = new ConfigResource(ConfigResource.Type.TOPIC, topicName);
                List<TopicOption> options =
                    admin
                        .describeConfigs(Arrays.asList(configKey))
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
                            admin.describeTopics(Arrays.asList(topicName)).values().get(topicName))
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
            });
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
                  admin.createPartitions(map).all().get(timeout.toMillis(), TimeUnit.MILLISECONDS));
        }
      }

      @Override
      public void deleteTopic(String topicName, Duration timeout) {
        CheckedExceptionUtil.wrap(
            () ->
                admin
                    .deleteTopics(Arrays.asList(topicName))
                    .all()
                    .get(timeout.toMillis(), TimeUnit.MILLISECONDS));
      }

      @Override
      public List<String> listTopics(Duration timeout) {
        return CheckedExceptionUtil.wrap(
            () ->
                admin
                    .listTopics()
                    .names()
                    .get(timeout.toMillis(), TimeUnit.MILLISECONDS)
                    .stream()
                    .collect(Collectors.toList()));
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
