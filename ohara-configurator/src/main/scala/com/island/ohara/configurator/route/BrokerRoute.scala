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
import com.island.ohara.agent.{ClusterCollie, NoSuchClusterException, NodeCollie}
import com.island.ohara.client.configurator.v0.BrokerApi
import com.island.ohara.client.configurator.v0.BrokerApi.{BrokerClusterCreationRequest, _}
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo

import scala.concurrent.{ExecutionContext, Future}
object BrokerRoute {

  def apply(implicit clusterCollie: ClusterCollie,
            nodeCollie: NodeCollie,
            executionContext: ExecutionContext): server.Route =
    RouteUtils.basicRouteOfCluster(
      collie = clusterCollie.brokerCollie(),
      root = BROKER_PREFIX_PATH,
      defaultImage = BrokerApi.IMAGE_NAME_DEFAULT,
      hookBeforeDelete = (clusters, name) =>
        CollieUtils
          .as[WorkerClusterInfo](clusters)
          .find(_.brokerClusterName == name)
          .map(c =>
            Future.failed(new IllegalArgumentException(
              s"you can't remove broker cluster:$name since it is used by worker cluster:${c.name}")))
          .getOrElse(Future.successful(name)),
      hookOfCreation = (clusters, req: BrokerClusterCreationRequest) => {
        val zkName = req.zookeeperClusterName
          .map { zkName =>
            clusters
              .filter(_.isInstanceOf[ZookeeperClusterInfo])
              .find(_.name == zkName)
              .map(_.name)
              .getOrElse(throw new NoSuchClusterException(s"zookeeper cluster:$zkName doesn't exist"))
          }
          .getOrElse {
            val zkClusters = clusters.filter(_.isInstanceOf[ZookeeperClusterInfo])
            zkClusters.size match {
              case 0 =>
                throw new IllegalArgumentException(
                  s"You didn't specify the zk cluster for bk cluster:${req.name}, and there is no default zk cluster")
              case 1 => zkClusters.head.name
              case _ =>
                throw new IllegalArgumentException(
                  s"You didn't specify the zk cluster for bk cluster ${req.name}, and there are too many zk clusters:{${zkClusters
                    .map(_.name)}}")
            }
          }
        val sameZkNameClusters = clusters
          .filter(_.isInstanceOf[BrokerClusterInfo])
          .map(_.asInstanceOf[BrokerClusterInfo])
          .filter(_.zookeeperClusterName == zkName)
        if (sameZkNameClusters.nonEmpty)
          throw new IllegalArgumentException(
            s"zk cluster:$zkName is already used by broker cluster:${sameZkNameClusters.head.name}")
        clusterCollie
          .brokerCollie()
          .creator()
          .clusterName(req.name)
          .clientPort(req.clientPort.getOrElse(BrokerApi.CLIENT_PORT_DEFAULT))
          .exporterPort(req.exporterPort.getOrElse(BrokerApi.EXPORTER_PORT_DEFAULT))
          .jmxPort(req.jmxPort.getOrElse(BrokerApi.JMX_PORT_DEFAULT))
          .zookeeperClusterName(zkName)
          .imageName(req.imageName.getOrElse(BrokerApi.IMAGE_NAME_DEFAULT))
          .nodeNames(req.nodeNames)
          .create()
      }
    )
}
