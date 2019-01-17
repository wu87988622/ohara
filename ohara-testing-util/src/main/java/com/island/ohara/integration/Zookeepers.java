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
import java.io.File;
import java.net.InetSocketAddress;
import java.util.Optional;
import org.apache.zookeeper.server.NIOServerCnxnFactory;
import org.apache.zookeeper.server.ZooKeeperServer;

public interface Zookeepers extends Releasable {

  String ZOOKEEPER_CONNECTION_PROPS = "ohara.it.zookeepers";

  /** @return zookeeper information. the form is "host_a:port_a,host_b:port_b" */
  String connectionProps();

  /** @return true if this zookeeper cluster is generated locally. */
  boolean isLocal();

  static Zookeepers local(int port) {
    final NIOServerCnxnFactory factory;
    File snapshotDir = CommonUtil.createTempDir("local-zk-snapshot");
    File logDir = CommonUtil.createTempDir("local-zk-log");

    try {
      factory = new NIOServerCnxnFactory();
      factory.configure(
          new InetSocketAddress(CommonUtil.anyLocalAddress(), Math.max(0, port)), 1024);
      factory.startup(new ZooKeeperServer(snapshotDir, logDir, 500));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return new Zookeepers() {
      @Override
      public void close() {
        factory.shutdown();
        CommonUtil.deleteFiles(snapshotDir);
        CommonUtil.deleteFiles(logDir);
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
                      public void close() {
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
        .orElseGet(() -> local(0));
  }
}
