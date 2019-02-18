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
import com.island.ohara.agent.{BrokerCollie, NoSuchClusterException, NodeCollie, WorkerCollie}
import com.island.ohara.client.configurator.v0.WorkerApi._
import com.island.ohara.configurator.jar.JarStore

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
object WorkerRoute {

  def apply(implicit brokerCollie: BrokerCollie,
            workerCollie: WorkerCollie,
            nodeCollie: NodeCollie,
            jarStore: JarStore): server.Route =
    RouteUtil.basicRouteOfCluster(
      root = WORKER_PREFIX_PATH,
      hookOfCreation = (req: WorkerClusterCreationRequest) =>
        // we need to handle the "default name" of broker cluster
        // this is important since user can't assign zk in ohara 0.2 so request won't carry the broker name.
        // we have to reassign the broker name manually.
        // TODO: remove this "friendly" helper in ohara 0.3
        req.brokerClusterName
          .map { bkName =>
            brokerCollie
              .exist(bkName)
              .flatMap(if (_) Future.successful(bkName)
              else Future.failed(new NoSuchClusterException(s"$bkName doesn't exist.")))
          }
          .getOrElse {
            brokerCollie.clusters().map { clusters =>
              if (clusters.size != 1)
                throw new IllegalArgumentException(
                  s"You didn't specify the broker cluster for ${req.name}, and there is no default broker cluster. actual:${clusters
                    .map(_._1.name)
                    .mkString(",")}")
              clusters.head._1.name
            }
          }
          .flatMap(bkName => jarStore.urls(req.jars).map(bkName -> _))
          .flatMap {
            case (bkName, urls) =>
              workerCollie
                .creator()
                .clusterName(req.name)
                .clientPort(req.clientPort)
                .brokerClusterName(bkName)
                .imageName(req.imageName)
                .jarUrls(urls)
                .nodeNames(req.nodeNames)
                .create()
        }
    )
}
