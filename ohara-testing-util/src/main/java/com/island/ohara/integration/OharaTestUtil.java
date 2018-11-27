package com.island.ohara.integration;

import com.island.ohara.client.ConnectorClient;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

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
public class OharaTestUtil implements AutoCloseable {
  private Database localDb;
  private ConnectorClient _connectorClient;
  private FtpServer localFtpServer;
  private Hdfs localHdfs;
  private Zookeepers zk;
  private Brokers brokers;
  private Workers workers;

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
        .map(b -> b.connectionProps())
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
        .map(w -> w.connectionProps())
        .orElseThrow(() -> new RuntimeException("Workers do not exist"));
  }

  // TODO ohara-common is scala module. This class for java not support apply method
  /*public ConnectorClient connectorClient() {
    // throw exception if there is no worker cluster
  }*/

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
  public void close() throws Exception {
    if (_connectorClient != null) {
      _connectorClient.close();
    }

    if (localDb != null) {
      localDb.close();
    }

    if (localFtpServer != null) {
      localFtpServer.close();
    }

    if (localHdfs != null) {
      localHdfs.close();
    }

    if (workers != null) {
      workers.close();
    }

    if (brokers != null) {
      brokers.close();
    }

    if (zk != null) {
      zk.close();
    }
  }

  /**
   * Create a test util with multi-brokers. NOTED: don't call the worker and hdfs service. otherwise
   * you will get exception
   *
   * @return a test util
   */
  public static OharaTestUtil brokers() {
    AtomicReference<Zookeepers> zk = new AtomicReference<>();
    Brokers brokers =
        Brokers.of(
            () -> {
              if (zk.get() == null) {
                zk.set(Zookeepers.of());
              }
              return zk.get();
            });
    return new OharaTestUtil(zk.get(), brokers, null);
  }

  /**
   * Create a test util with multi-brokers and multi-workers. NOTED: don't call the hdfs service.
   * otherwise you will get exception
   *
   * @return a test util
   */
  public static OharaTestUtil workers() {
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
                        }));
              }
              return brokers.get();
            });
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

  static String HELP_KEY = "--help";
  static String TTL_KEY = "--ttl";
  static String USAGE = "[Usage]" + TTL_KEY;

  public static void main(String args[]) {
    if (args.length == 1 && args[0] == HELP_KEY) {
      System.out.println(USAGE);
      return;
    }
    if (args.length % 2 != 0) {
      throw new IllegalArgumentException(USAGE);
    }

    AtomicReference<Long> ttl = new AtomicReference<>();
    ttl.set(9999L);
    /*args.sliding(2, 2).foreach {
      case Array(TTL_KEY, value) => ttl = value.toInt
      case _                     => throw new IllegalArgumentException(USAGE)
    }*/

    IntStream.range(0, args.length)
        .forEach(
            index -> {
              if (args[index].equals(TTL_KEY)) {
                ttl.set(Long.parseLong(args[index + 1]));
              }
            });

    Brokers brokers = OharaTestUtil.workers().brokers;
    Workers workers = OharaTestUtil.workers().workers;
    System.out.println("wait for the mini kafka cluster");
    try {
      TimeUnit.SECONDS.sleep(5);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    System.out.println(
        "Succeed to run the mini brokers: "
            + brokers.connectionProps()
            + " and workers: "
            + workers.connectionProps());

    System.out.println(
        "enter ctrl+c to terminate the mini broker cluster (or the cluster will be terminated after "
            + ttl
            + " seconds");
    try {
      TimeUnit.SECONDS.sleep(ttl.get());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
