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
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.ZookeeperApi
import com.island.ohara.client.configurator.v0.ZookeeperApi._

import scala.concurrent.Future

object ZookeeperRoute {

  def apply(implicit clusterCollie: ClusterCollie, nodeCollie: NodeCollie): server.Route =
    RouteUtils.basicRouteOfCluster(
      collie = clusterCollie.zookeeperCollie(),
      root = ZOOKEEPER_PREFIX_PATH,
      defaultImage = ZookeeperApi.IMAGE_NAME_DEFAULT,
      hookBeforeDelete = (clusters, name) =>
        CollieUtils
          .as[BrokerClusterInfo](clusters)
          .find(_.zookeeperClusterName == name)
          .map(c =>
            Future.failed(new IllegalArgumentException(
              s"you can't remove zookeeper cluster:$name since it is used by broker cluster:${c.name}")))
          .getOrElse(Future.successful(name)),
      hookOfCreation = (_, req: ZookeeperClusterCreationRequest) =>
        clusterCollie
          .zookeeperCollie()
          .creator()
          .clusterName(req.name)
          .clientPort(req.clientPort.getOrElse(ZookeeperApi.CLIENT_PORT_DEFAULT))
          .electionPort(req.electionPort.getOrElse(ZookeeperApi.ELECTION_PORT_DEFAULT))
          .peerPort(req.peerPort.getOrElse(ZookeeperApi.PEER_PORT_DEFAULT))
          .imageName(req.imageName.getOrElse(ZookeeperApi.IMAGE_NAME_DEFAULT))
          .nodeNames(req.nodeNames)
          .create()
    )
}
