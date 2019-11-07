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
import com.island.ohara.agent.ServiceCollie
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterStatus
import com.island.ohara.client.configurator.v0.ContainerApi._
import com.island.ohara.client.configurator.v0.StreamApi.StreamClusterStatus
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterStatus
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterStatus
import com.island.ohara.common.setting.ObjectKey
import spray.json.DefaultJsonProtocol._

import scala.concurrent.ExecutionContext

object ContainerRoute {

  def apply(implicit serviceCollie: ServiceCollie, executionContext: ExecutionContext): server.Route =
    path(CONTAINER_PREFIX_PATH / Segment)({ clusterName =>
      parameter(GROUP_KEY ? GROUP_DEFAULT) {
        group =>
          get {
            complete(
              serviceCollie
                .clusters()
                .map(_.filter(_._1.key == ObjectKey.of(group, clusterName)).map {
                  case (cluster, containers) =>
                    ContainerGroup(
                      clusterKey = ObjectKey.of(group, clusterName),
                      clusterType = cluster match {
                        case _: ZookeeperClusterStatus => "zookeeper"
                        case _: BrokerClusterStatus    => "broker"
                        case _: WorkerClusterStatus    => "worker"
                        case _: StreamClusterStatus    => "stream"
                        case _                         => "unknown"
                      },
                      containers = containers
                    )
                }))
          }
      }
    })
}
