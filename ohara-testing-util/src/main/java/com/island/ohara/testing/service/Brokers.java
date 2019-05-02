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

package com.island.ohara.testing.service;

import com.island.ohara.common.util.CommonUtils;
import com.island.ohara.common.util.Releasable;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import kafka.server.KafkaConfig;
import kafka.server.KafkaServer;
import org.apache.kafka.common.network.ListenerName;
import org.apache.kafka.common.utils.SystemTime;

public interface Brokers extends Releasable {
  /** @return brokers information. the form is "host_a:port_a,host_b:port_b" */
  String connectionProps();

  /** @return true if this broker cluster is generated locally. */
  boolean isLocal();

  static Brokers local(Zookeepers zk, int[] ports) {
    List<File> tempFolders =
        IntStream.range(0, ports.length)
            .mapToObj(i -> CommonUtils.createTempFolder("local_kafka"))
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
        brokers.stream()
            .map(
                broker ->
                    CommonUtils.hostname() + ":" + broker.boundPort(new ListenerName("PLAINTEXT")))
            .collect(Collectors.joining(","));
    return new Brokers() {

      @Override
      public void close() {
        brokers.forEach(
            broker -> {
              broker.shutdown();
              broker.awaitShutdown();
            });
        tempFolders.forEach(CommonUtils::deleteFiles);
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
}
