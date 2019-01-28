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
import com.island.ohara.client.configurator.v0.ClusterInfo
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.common.util.Releasable

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * This is the top-of-the-range "collie". It maintains and organizes all collies.
  * Each getter should return new instance of collie since each collie has close() method.
  * However, it is ok to keep global instance of collie if they have dump close().
  * Currently, default implementation is based on ssh and docker command. It is simple but slow.
  * TODO: We are looking for k8s implementation...by chia
  */
trait ClusterCollie extends Releasable {

  /**
    * create a collie for zookeeper cluster
    * @return zookeeper collie
    */
  def zookeepersCollie(): ZookeeperCollie

  /**
    * create a collie for broker cluster
    * @return broker collie
    */
  def brokerCollie(): BrokerCollie

  /**
    * create a collie for worker cluster
    * @return worker collie
    */
  def workerCollie(): WorkerCollie

  /**
    * the default implementation is expensive!!! Please override this method if you are a good programmer.
    * @return a collection of zk, bk and wk clusters
    */
  def clusters(): Future[Map[ClusterInfo, Seq[ContainerInfo]]] = zookeepersCollie().clusters().flatMap { zkMap =>
    brokerCollie().clusters().flatMap { bkMap =>
      workerCollie().clusters().map { wkMap =>
        wkMap.map {
          case (wk, wkContainers) => (wk.asInstanceOf[ClusterInfo], wkContainers)
        } ++ bkMap.map {
          case (bk, bkContainers) => (bk.asInstanceOf[ClusterInfo], bkContainers)
        } ++ zkMap.map {
          case (zk, zkContainers) => (zk.asInstanceOf[ClusterInfo], zkContainers)
        }
      }
    }
  }
}

object ClusterCollie {
  def apply(implicit nodeCollie: NodeCollie): ClusterCollie = ssh

  /**
    * the default implementation uses ssh and docker command to manage all clusters.
    * Each node running the service has name "${clusterName}-${service}-${index}".
    * For example, there is a worker cluster called "workercluster" and it is run on 3 nodes.
    * node-0 => workercluster-worker-0
    * node-1 => workercluster-worker-1
    * node-2 => workercluster-worker-2
    */
  def ssh(implicit nodeCollie: NodeCollie): ClusterCollie = new ClusterCollieImpl

  /**
    * create kubernetes implements
    * @param nodeCollie
    * @return
    */
  def k8s(implicit nodeCollie: NodeCollie, k8sClient: K8SClient): ClusterCollie = new K8SClusterCollieImpl
}
