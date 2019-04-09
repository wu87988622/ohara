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
import com.island.ohara.client.configurator.v0.BrokerApi.BROKER_PREFIX_PATH
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.{LogApi, Parameters}
import com.island.ohara.client.configurator.v0.WorkerApi.WORKER_PREFIX_PATH
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZOOKEEPER_PREFIX_PATH
import com.island.ohara.client.configurator.v0.LogApi._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Used to take log from specified cluster. We haven't log infra to provide UI to get log from specified "connector".
  * However, users need to "see" what happen on failed connectors. We don't implement the LogApi (client library) since
  * this is just a workaround.
  */
object LogRoute {

  private[this] def route(clusterName: String, data: Future[Map[ContainerInfo, String]])(
    implicit executionContext: ExecutionContext): server.Route =
    complete(data.map { d =>
      if (d.isEmpty)
        ClusterLog(
          name = clusterName,
          logs = Seq(
            NodeLog(
              name = "N/A",
              value = "You have to foot the bill to use this fucking advanced function!!!"
            ))
        )
      else
        ClusterLog(
          name = clusterName,
          logs = d.map {
            case (container, log) => NodeLog(container.nodeName, log)
          }.toSeq
        )
    })

  def apply(implicit collie: ClusterCollie, executionContext: ExecutionContext): server.Route =
    pathPrefix(LogApi.LOG_PREFIX_PATH) {
      pathPrefix(ZOOKEEPER_PREFIX_PATH) {
        path(Segment) { zkClusterName =>
          route(zkClusterName, collie.zookeeperCollie().logs(zkClusterName))
        } ~ pathEnd(parameter(Parameters.CLUSTER_NAME) { zkClusterName =>
          // TODO: this api is deprecated
          route(zkClusterName, collie.zookeeperCollie().logs(zkClusterName))
        })
      } ~ pathPrefix(BROKER_PREFIX_PATH) {
        path(Segment) { bkClusterName =>
          route(bkClusterName, collie.brokerCollie().logs(bkClusterName))
        } ~ pathEnd(parameter(Parameters.CLUSTER_NAME) { bkClusterName =>
          // TODO: this api is deprecated
          route(bkClusterName, collie.brokerCollie().logs(bkClusterName))
        })
      } ~ pathPrefix(WORKER_PREFIX_PATH) {
        path(Segment) { wkClusterName =>
          route(wkClusterName, collie.workerCollie().logs(wkClusterName))
        } ~ pathEnd(parameter(Parameters.CLUSTER_NAME) { wkClusterName =>
          // TODO: this api is deprecated
          route(wkClusterName, collie.workerCollie().logs(wkClusterName))
        })
      }
    }
}
