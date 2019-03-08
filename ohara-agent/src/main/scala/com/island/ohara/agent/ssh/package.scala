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

package com.island.ohara.agent

import com.island.ohara.client.kafka.WorkerClient
import com.island.ohara.client.kafka.WorkerJson.Plugin
import com.typesafe.scalalogging.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
package object ssh {
  private[ssh] val LOG = Logger("SshClusterCollie")

  /**
    * It tried to fetch connector information from starting worker cluster
    * However, it may be too slow to get latest connector information.
    * We don't throw exception since it is a common case, and Skipping retry can make quick response
    * @param connectionProps worker connection props
    * @return plugin description or nothing
    */
  private[ssh] def plugins(connectionProps: String): Future[Seq[Plugin]] =
    WorkerClient(connectionProps, maxRetry = 0).plugins().recover {
      case e: Throwable =>
        LOG.error(s"Failed to fetch connectors information of cluster:$connectionProps. Use empty list instead", e)
        Seq.empty
    }

  /**
    * We need this prefix in order to distinguish our containers from others.
    * DON'T change this constant string. Otherwise, it will break compatibility.
    * We don't use a complex string since docker limit the length of name...
    */
  private[ssh] val PREFIX_KEY = "occl"

  /**
    * internal key used to save the broker cluster name.
    * All nodes of worker cluster should have this environment variable.
    */
  private[ssh] val BROKER_CLUSTER_NAME = "CCI_BROKER_CLUSTER_NAME"

  /**
    * internal key used to save the zookeeper cluster name.
    * All nodes of broker cluster should have this environment variable.
    */
  private[ssh] val ZOOKEEPER_CLUSTER_NAME = "CCI_ZOOKEEPER_CLUSTER_NAME"

  /**
    * used to distinguish the cluster name and service name
    */
  private[ssh] val DIVIDER: String = "-"

  private[ssh] val LENGTH_OF_CONTAINER_NAME_ID: Int = 7

  /**
    * In ssh mode we use host driver to mount /etc/hosts from container host.
    */
  private[ssh] val NETWORK_DRIVER: NetworkDriver = NetworkDriver.HOST

  private[ssh] val ZK_SERVICE_NAME = "zk"
  private[ssh] val BK_SERVICE_NAME = "bk"
  private[ssh] val WK_SERVICE_NAME = "wk"
}
