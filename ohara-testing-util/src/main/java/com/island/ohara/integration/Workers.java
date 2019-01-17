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

package com.island.ohara.integration;

import com.island.ohara.common.util.CommonUtil;
import com.island.ohara.common.util.Releasable;
import java.net.BindException;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.utils.Time;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.runtime.Connect;
import org.apache.kafka.connect.runtime.Worker;
import org.apache.kafka.connect.runtime.WorkerConfig;
import org.apache.kafka.connect.runtime.distributed.DistributedConfig;
import org.apache.kafka.connect.runtime.distributed.DistributedHerder;
import org.apache.kafka.connect.runtime.isolation.Plugins;
import org.apache.kafka.connect.runtime.rest.RestServer;
import org.apache.kafka.connect.storage.Converter;
import org.apache.kafka.connect.storage.KafkaConfigBackingStore;
import org.apache.kafka.connect.storage.KafkaOffsetBackingStore;
import org.apache.kafka.connect.storage.KafkaStatusBackingStore;

public interface Workers extends Releasable {
  String WORKER_CONNECTION_PROPS = "ohara.it.workers";

  int NUMBER_OF_WORKERS = 3;

  /** @return workers information. the form is "host_a:port_a,host_b:port_b" */
  String connectionProps();

  /** @return true if this worker cluster is generated locally. */
  boolean isLocal();

  static Workers local(Brokers brokers, int[] ports) {
    List<Integer> availablePorts = new ArrayList<>(ports.length);
    List<Connect> connects =
        Arrays.stream(ports)
            .mapToObj(
                port -> {
                  boolean canRetry = port <= 0;
                  while (true) {
                    try {
                      int availablePort = CommonUtil.resolvePort(port);

                      Map<String, String> config = new HashMap<>();
                      // reduce the number from partitions and replicas to speedup the mini cluster
                      // for config storage. the partition from config topic is always 1 so we
                      // needn't to set it to 1 here.
                      config.put(DistributedConfig.CONFIG_TOPIC_CONFIG, "connect-configs");
                      config.put(DistributedConfig.CONFIG_STORAGE_REPLICATION_FACTOR_CONFIG, "1");
                      // for offset storage
                      config.put(DistributedConfig.OFFSET_STORAGE_TOPIC_CONFIG, "connect-offsets");
                      config.put(DistributedConfig.OFFSET_STORAGE_PARTITIONS_CONFIG, "1");
                      config.put(DistributedConfig.OFFSET_STORAGE_REPLICATION_FACTOR_CONFIG, "1");
                      // for status storage
                      config.put(DistributedConfig.STATUS_STORAGE_TOPIC_CONFIG, "connect-status");
                      config.put(DistributedConfig.STATUS_STORAGE_PARTITIONS_CONFIG, "1");
                      config.put(DistributedConfig.STATUS_STORAGE_REPLICATION_FACTOR_CONFIG, "1");
                      // set the brokers info
                      config.put(
                          CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokers.connectionProps());
                      config.put(DistributedConfig.GROUP_ID_CONFIG, "connect");
                      // set the normal converter
                      config.put(
                          WorkerConfig.KEY_CONVERTER_CLASS_CONFIG,
                          "org.apache.kafka.connect.json.JsonConverter");
                      config.put("key.converter.schemas.enable", "true");
                      config.put(
                          WorkerConfig.VALUE_CONVERTER_CLASS_CONFIG,
                          "org.apache.kafka.connect.json.JsonConverter");
                      config.put("value.converter.schemas.enable", "true");
                      // set the internal converter. NOTED: kafka connector doesn't support to use
                      // schema in internal topics.
                      config.put(
                          WorkerConfig.INTERNAL_KEY_CONVERTER_CLASS_CONFIG,
                          "org.apache.kafka.connect.json.JsonConverter");
                      config.put("internal.key.converter.schemas.enable", "false");
                      config.put(
                          WorkerConfig.INTERNAL_VALUE_CONVERTER_CLASS_CONFIG,
                          "org.apache.kafka.connect.json.JsonConverter");
                      config.put("internal.value.converter.schemas.enable", "false");
                      // TODO: REST_PORT_CONFIG is deprecated in kafka-1.1.0. Use LISTENERS_CONFIG
                      // instead. by chia
                      config.put(WorkerConfig.REST_PORT_CONFIG, String.valueOf(availablePort));

                      DistributedConfig distConfig = new DistributedConfig(config);
                      RestServer rest = new RestServer(distConfig);
                      String workerId =
                          CommonUtil.hostname() + ":" + rest.advertisedUrl().getPort();
                      KafkaOffsetBackingStore offsetBackingStore = new KafkaOffsetBackingStore();
                      offsetBackingStore.configure(distConfig);
                      Time time = Time.SYSTEM;
                      Worker worker =
                          new Worker(
                              workerId,
                              time,
                              new Plugins(Collections.emptyMap()),
                              distConfig,
                              offsetBackingStore);
                      Converter internalValueConverter = worker.getInternalValueConverter();
                      KafkaStatusBackingStore statusBackingStore =
                          new KafkaStatusBackingStore(time, internalValueConverter);
                      statusBackingStore.configure(distConfig);
                      KafkaConfigBackingStore configBackingStore =
                          new KafkaConfigBackingStore(internalValueConverter, distConfig);
                      // TODO: DistributedHerder is a private class so its constructor is changed in
                      // kafka-1.1.0. by chia
                      DistributedHerder herder =
                          new DistributedHerder(
                              distConfig,
                              time,
                              worker,
                              statusBackingStore,
                              configBackingStore,
                              rest.advertisedUrl().toString());
                      Connect connect = new Connect(herder, rest);
                      connect.start();
                      availablePorts.add(availablePort);
                      return connect;
                    } catch (ConnectException e) {
                      if (!canRetry || !(e.getCause() instanceof BindException)) throw e;
                    }
                  }
                })
            .collect(Collectors.toList());
    return new Workers() {
      @Override
      public void close() {
        connects.forEach(Connect::stop);
        connects.forEach(Connect::awaitStop);
      }

      @Override
      public String connectionProps() {
        return availablePorts
            .stream()
            .map(p -> CommonUtil.hostname() + ":" + p)
            .collect(Collectors.joining(","));
      }

      @Override
      public boolean isLocal() {
        return true;
      }
    };
  }

  static Workers of(Supplier<Brokers> brokers, int numberOfWorkers) {
    return of(System.getenv(WORKER_CONNECTION_PROPS), brokers, numberOfWorkers);
  }

  static Workers of(String workers, Supplier<Brokers> brokers, int numberOfWorkers) {
    return Optional.ofNullable(workers)
        .map(
            w ->
                (Workers)
                    new Workers() {
                      @Override
                      public void close() {
                        // Nothing
                      }

                      @Override
                      public String connectionProps() {
                        return w;
                      }

                      @Override
                      public boolean isLocal() {
                        return false;
                      }
                    })
        .orElseGet(
            () -> local(brokers.get(), IntStream.range(0, numberOfWorkers).map(x -> 0).toArray()));
  }
}
