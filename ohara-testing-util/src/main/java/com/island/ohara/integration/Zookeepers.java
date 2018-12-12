package com.island.ohara.integration;

import com.island.ohara.common.util.CommonUtil;
import java.io.File;
import java.net.InetSocketAddress;
import java.util.Optional;
import org.apache.zookeeper.server.NIOServerCnxnFactory;
import org.apache.zookeeper.server.ZooKeeperServer;

public interface Zookeepers extends AutoCloseable {

  String ZOOKEEPER_CONNECTION_PROPS = "ohara.it.zookeepers";

  /** @return zookeeper information. the form is "host_a:port_a,host_b:port_b" */
  String connectionProps();

  /** @return true if this zookeeper cluster is generated locally. */
  boolean isLocal();

  static Zookeepers local(Integer port) {
    final NIOServerCnxnFactory factory;
    File snapshotDir = Integration.createTempDir("standalone-zk/snapshot");
    File logDir = Integration.createTempDir("standalone-zk/log");

    try {
      factory = new NIOServerCnxnFactory();
      factory.configure(new InetSocketAddress(CommonUtil.anyLocalAddress(), port), 1024);
      factory.startup(new ZooKeeperServer(snapshotDir, logDir, 500));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return new Zookeepers() {
      @Override
      public void close() throws Exception {
        factory.shutdown();
        Integration.deleteFiles(snapshotDir);
        Integration.deleteFiles(logDir);
      }

      @Override
      public String connectionProps() {
        return CommonUtil.hostname() + ":" + factory.getLocalPort();
      }

      @Override
      public boolean isLocal() {
        return true;
      }
    };
  }

  static Zookeepers of() {
    return of(System.getenv(ZOOKEEPER_CONNECTION_PROPS));
  }

  static Zookeepers of(String zookeepers) {
    return Optional.ofNullable(zookeepers)
        .map(
            s ->
                (Zookeepers)
                    new Zookeepers() {
                      @Override
                      public void close() throws Exception {
                        // Nothing
                      }

                      @Override
                      public String connectionProps() {
                        return s;
                      }

                      @Override
                      public boolean isLocal() {
                        return false;
                      }
                    })
        .orElseGet(() -> local(Integration.freePort()));
  }
}
