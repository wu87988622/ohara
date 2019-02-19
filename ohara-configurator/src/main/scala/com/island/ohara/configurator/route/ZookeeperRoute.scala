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
import com.island.ohara.agent.{ClusterCollie, NodeCollie}
import com.island.ohara.client.configurator.v0.ZookeeperApi._

import scala.concurrent.ExecutionContext.Implicits.global

object ZookeeperRoute {

  def apply(implicit clusterCollie: ClusterCollie, nodeCollie: NodeCollie): server.Route =
    RouteUtil.basicRouteOfCluster(
      collie = clusterCollie.zookeeperCollie(),
      root = ZOOKEEPER_PREFIX_PATH,
      hookOfCreation = (req: ZookeeperClusterCreationRequest) =>
        clusterCollie.clusters().flatMap { clusters =>
          if (clusters.keys
                .filter(_.isInstanceOf[ZookeeperClusterInfo])
                .map(_.asInstanceOf[ZookeeperClusterInfo])
                .exists(_.name == req.name))
            throw new IllegalArgumentException(s"zookeeper cluster:${req.name} is running")
          clusterCollie
            .zookeeperCollie()
            .creator()
            .clusterName(req.name)
            .clientPort(req.clientPort)
            .electionPort(req.electionPort)
            .peerPort(req.peerPort)
            .imageName(req.imageName)
            .nodeNames(req.nodeNames)
            .create()
      }
    )
}
