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

import com.island.ohara.common.util.Releasable;
import com.island.ohara.common.util.ReleaseOnce;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This class create a kafka services having 1 zk instance and 1 broker default. Also, this class
 * have many helper methods to make test more friendly.
 *
 * <p>How to use this class: 1) create the OharaTestUtil with 1 broker (you can assign arbitrary
 * number from brokers) val testUtil = OharaTestUtil.localBrokers(1) 2) get the
 * basic|producer|consumer OharaConfiguration val config = testUtil.producerConfig 3) instantiate
 * your producer or consumer val producer = new KafkaProducer<Array<Byte>, Array<Byte>>(config, new
 * ByteArraySerializer, new ByteArraySerializer) 4) do what you want for your producer and consumer
 * ... 5) close OharaTestUtil testUtil.close()
 *
 * <p>see TestOharaTestUtil for more examples NOTED: the close() will shutdown all services
 * including the passed consumers (see run())
 */
public class OharaTestUtil extends ReleaseOnce {
  private Database localDb;
  private FtpServer localFtpServer;
  private Hdfs localHdfs;
  private final Zookeepers zk;
  private final Brokers brokers;
  private final Workers workers;

  private OharaTestUtil(Zookeepers zk, Brokers brokers, Workers workers) {
    this.zk = zk;
    this.brokers = brokers;
    this.workers = workers;
  }

  /**
   * Exposing the brokers connection. This list should be in the form <code>
   * host1:port1,host2:port2,...</code>.
   *
   * @return brokers connection information
   */
  public String brokersConnProps() {
    return Optional.ofNullable(brokers)
        .map(Brokers::connectionProps)
        .orElseThrow(
            () ->
                new RuntimeException(
                    "Brokers do not exist. Because Workers exist in supply environment, then we don't create embedded Brokers. Please do not operate it"));
  }

  /**
   * Exposing the workers connection. This list should be in the form <code>
   * host1:port1,host2:port2,...</code>.
   *
   * @return workers connection information
   */
  public String workersConnProps() {
    return Optional.ofNullable(workers)
        .map(Workers::connectionProps)
        .orElseThrow(() -> new RuntimeException("Workers do not exist"));
  }

  public Hdfs hdfs() {
    if (localHdfs == null) localHdfs = Hdfs.of();
    return localHdfs;
  }

  public Database dataBase() {
    if (localDb == null) localDb = Database.of();
    return localDb;
  }

  public FtpServer ftpServer() {
    if (localFtpServer == null) localFtpServer = FtpServer.of();
    return localFtpServer;
  }

  @Override
  protected void doClose() {
    Releasable.close(localDb);
    Releasable.close(localFtpServer);
    Releasable.close(localHdfs);
    Releasable.close(workers);
    Releasable.close(brokers);
    Releasable.close(zk);
  }

  /**
   * create a test util with a broker cluster based on single node. NOTED: don't call the worker and
   * hdfs service. otherwise you will get exception
   *
   * @return a test util
   */
  public static OharaTestUtil broker() {
    return brokers(1);
  }

  /**
   * Create a test util with multi-brokers. NOTED: don't call the worker and hdfs service. otherwise
   * you will get exception
   *
   * @return a test util
   */
  public static OharaTestUtil brokers(int numberOfBrokers) {
    AtomicReference<Zookeepers> zk = new AtomicReference<>();
    Brokers brokers =
        Brokers.of(
            () -> {
              if (zk.get() == null) {
                zk.set(Zookeepers.of());
              }
              return zk.get();
            },
            numberOfBrokers);
    return new OharaTestUtil(zk.get(), brokers, null);
  }

  /**
   * create a test util with a worker/broker cluster based on single node. NOTED: don't call the
   * worker and hdfs service. otherwise you will get exception
   *
   * @return a test util
   */
  public static OharaTestUtil worker() {
    return workers(1);
  }

  /**
   * Create a test util with multi-brokers and multi-workers. NOTED: don't call the hdfs service.
   * otherwise you will get exception.
   *
   * <p>NOTED: the default number of brokers is 3
   *
   * @return a test util
   */
  public static OharaTestUtil workers(int numberOfWorkers) {
    AtomicReference<Zookeepers> zk = new AtomicReference<>();
    AtomicReference<Brokers> brokers = new AtomicReference<>();

    Workers workers =
        Workers.of(
            () -> {
              if (brokers.get() == null) {
                brokers.set(
                    Brokers.of(
                        () -> {
                          if (zk.get() == null) zk.set(Zookeepers.of());
                          return zk.get();
                        },
                        numberOfWorkers));
              }
              return brokers.get();
            },
            numberOfWorkers);
    return new OharaTestUtil(zk.get(), brokers.get(), workers);
  }

  /**
   * Create a test util with local file system. NOTED: don't call the workers and brokers service.
   * otherwise you will get exception
   *
   * @return a test util
   */
  public static OharaTestUtil localHDFS() {
    return new OharaTestUtil(null, null, null);
  }
}
