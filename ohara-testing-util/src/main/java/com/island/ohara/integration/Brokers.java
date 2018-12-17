package com.island.ohara.integration;

import com.island.ohara.common.util.CommonUtil;
import com.island.ohara.common.util.Releasable;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import kafka.server.KafkaConfig;
import kafka.server.KafkaServer;
import org.apache.kafka.common.network.ListenerName;
import org.apache.kafka.common.utils.SystemTime;

public interface Brokers extends Releasable {
  String BROKER_CONNECTION_PROPS = "ohara.it.brokers";

  /** @return brokers information. the form is "host_a:port_a,host_b:port_b" */
  String connectionProps();

  /** @return true if this broker cluster is generated locally. */
  boolean isLocal();

  static Brokers local(Zookeepers zk, int[] ports) {
    List<File> tempFolders =
        IntStream.range(0, ports.length)
            .mapToObj(i -> Integration.createTempDir("kafka-local"))
            .collect(Collectors.toList());
    List<KafkaServer> brokers =
        IntStream.range(0, ports.length)
            .mapToObj(
                index -> {
                  int port = ports[index];
                  File logDir = tempFolders.get(index);
                  Properties config = new Properties();
                  // reduce the number from partitions and replicas to speedup the mini cluster
                  // It is too hard to access scala's object member from java so we use "string"
                  // instead
                  // reference to KafkaConfig.OffsetsTopicPartitionsProp
                  config.setProperty("offsets.topic.num.partitions", String.valueOf(1));
                  // reference to KafkaConfig.OffsetsTopicReplicationFactorProp
                  config.setProperty("offsets.topic.replication.factor", String.valueOf(1));
                  // reference to KafkaConfig.ZkConnectProp
                  config.setProperty("zookeeper.connect", zk.connectionProps());
                  // reference to KafkaConfig.BrokerIdProp
                  config.setProperty("broker.id", String.valueOf(index));
                  // reference to KafkaConfig.ListenersProp
                  config.setProperty("listeners", "PLAINTEXT://:" + (port <= 0 ? 0 : port));
                  // reference to KafkaConfig.LogDirProp
                  config.setProperty("log.dir", logDir.getAbsolutePath());
                  // reference to KafkaConfig.ZkConnectionTimeoutMsProp
                  // increase the timeout in order to avoid ZkTimeoutException
                  config.setProperty("zookeeper.connection.timeout.ms", String.valueOf(30 * 1000));
                  KafkaServer broker =
                      new KafkaServer(
                          new KafkaConfig(config),
                          SystemTime.SYSTEM,
                          scala.Option.apply(null),
                          scala.collection.JavaConversions.asScalaBuffer(Collections.emptyList()));
                  broker.startup();
                  return broker;
                })
            .collect(Collectors.toList());
    String connectionProps =
        brokers
            .stream()
            .map(
                broker ->
                    CommonUtil.hostname() + ":" + broker.boundPort(new ListenerName("PLAINTEXT")))
            .collect(Collectors.joining(","));
    return new Brokers() {

      @Override
      public void close() {
        brokers.forEach(
            broker -> {
              broker.shutdown();
              broker.awaitShutdown();
            });
        tempFolders.forEach(Integration::deleteFiles);
      }

      @Override
      public String connectionProps() {
        return connectionProps;
      }

      @Override
      public boolean isLocal() {
        return true;
      }
    };
  }

  static Brokers of(Supplier<Zookeepers> zk, int numberOfBrokers) {
    return of(System.getenv(BROKER_CONNECTION_PROPS), zk, numberOfBrokers);
  }

  static Brokers of(String brokers, Supplier<Zookeepers> zk, int numberOfBrokers) {
    return Optional.ofNullable(brokers)
        .map(
            s ->
                (Brokers)
                    new Brokers() {
                      @Override
                      public void close() {
                        // Nothing
                      }

                      @Override
                      public String connectionProps() {
                        if (s.split(",").length != numberOfBrokers)
                          throw new IllegalArgumentException(
                              "Expected number of brokers is "
                                  + numberOfBrokers
                                  + " but actual is "
                                  + s.split(",").length
                                  + "("
                                  + s
                                  + ")");
                        return s;
                      }

                      @Override
                      public boolean isLocal() {
                        return false;
                      }
                    })
        .orElseGet(
            () -> local(zk.get(), IntStream.range(0, numberOfBrokers).map(i -> 0).toArray()));
  }
}
