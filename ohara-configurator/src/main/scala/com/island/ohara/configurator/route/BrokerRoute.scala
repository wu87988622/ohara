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

package com.island.ohara.configurator.route

import akka.http.scaladsl.server
import com.island.ohara.agent.{BrokerCollie, ClusterCollie, NoSuchClusterException, NodeCollie}
import com.island.ohara.client.configurator.v0.BrokerApi.{BrokerClusterCreationRequest, _}
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo

import scala.concurrent.ExecutionContext.Implicits.global
object BrokerRoute {

  def apply(implicit brokerCollie: BrokerCollie, collie: ClusterCollie, nodeCollie: NodeCollie): server.Route =
    RouteUtil.basicRouteOfCluster(
      root = BROKER_PREFIX_PATH,
      hookOfCreation = (req: BrokerClusterCreationRequest) =>
        // we need to handle the "default name" of zookeeper cluster
        // this is important since user can't assign zk in ohara 0.2 so request won't carry the zk name.
        // we have to reassign the zk name manually.
        // TODO: remove this "friendly" helper in ohara 0.3
        collie
          .clusters()
          .map { clusters =>
            val zkName = req.zookeeperClusterName
              .map { zkName =>
                clusters.keys
                  .filter(_.isInstanceOf[ZookeeperClusterInfo])
                  .find(_.name == zkName)
                  .map(_.name)
                  .getOrElse(throw new NoSuchClusterException(s"$zkName doesn't exist"))
              }
              .getOrElse {
                val zkClusters = clusters.keys.filter(_.isInstanceOf[ZookeeperClusterInfo])
                if (zkClusters.size != 1)
                  throw new IllegalArgumentException(
                    s"You didn't specify the zk cluster for ${req.name}, and there is no default zk cluster")
                zkClusters.head.name
              }
            val sameNameClusters = clusters.keys
              .filter(_.isInstanceOf[BrokerClusterInfo])
              .map(_.asInstanceOf[BrokerClusterInfo])
              .filter(_.zookeeperClusterName == zkName)
            if (sameNameClusters.nonEmpty)
              throw new IllegalArgumentException(
                s"zk cluster:$zkName is already used by ${sameNameClusters.map(_.name)}")
            zkName
          }
          .flatMap { zkName =>
            brokerCollie
              .creator()
              .clusterName(req.name)
              .clientPort(req.clientPort)
              .exporterPort(req.exporterPort)
              .zookeeperClusterName(zkName)
              .imageName(req.imageName)
              .nodeNames(req.nodeNames)
              .create()
        }
    )
}
