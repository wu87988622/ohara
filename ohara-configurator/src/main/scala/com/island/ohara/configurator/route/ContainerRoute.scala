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
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import com.island.ohara.agent.ClusterCollie
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.ContainerApi._
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo
import spray.json.DefaultJsonProtocol._

import scala.concurrent.ExecutionContext

object ContainerRoute {

  def apply(implicit clusterCollie: ClusterCollie, executionContext: ExecutionContext): server.Route =
    path(CONTAINER_PREFIX_PATH / Segment)({ clusterName =>
      get {
        complete(clusterCollie.clusters.map(_.filter(_._1.name == clusterName).map {
          case (cluster, containers) =>
            ContainerGroup(
              clusterName = clusterName,
              clusterType = cluster match {
                case _: ZookeeperClusterInfo => "zookeeper"
                case _: BrokerClusterInfo    => "broker"
                case _: WorkerClusterInfo    => "worker"
                case _                       => "unknown"
              },
              containers = containers
            )
        }))
      }
    })
}
