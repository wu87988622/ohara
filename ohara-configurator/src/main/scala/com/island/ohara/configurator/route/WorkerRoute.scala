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
import com.island.ohara.agent._
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.WorkerApi._
import com.island.ohara.configurator.jar.JarStore

import scala.concurrent.ExecutionContext.Implicits.global
object WorkerRoute {

  def apply(implicit clusterCollie: ClusterCollie, nodeCollie: NodeCollie, jarStore: JarStore): server.Route =
    RouteUtil.basicRouteOfCluster(
      collie = clusterCollie.workerCollie(),
      root = WORKER_PREFIX_PATH,
      hookOfCreation = (req: WorkerClusterCreationRequest) =>
        // we need to handle the "default name" of broker cluster
        // this is important since user can't assign zk in ohara 0.2 so request won't carry the broker name.
        // we have to reassign the broker name manually.
        // TODO: remove this "friendly" helper in ohara 0.3
        clusterCollie
          .clusters()
          .map { clusters =>
            if (clusters.keys
                  .filter(_.isInstanceOf[WorkerClusterInfo])
                  .map(_.asInstanceOf[WorkerClusterInfo])
                  .exists(_.name == req.name))
              throw new IllegalArgumentException(s"worker cluster:${req.name} is running")
            req.brokerClusterName
              .map { bkName =>
                clusters.keys
                  .filter(_.isInstanceOf[BrokerClusterInfo])
                  .find(_.name == bkName)
                  .map(_.name)
                  .getOrElse(throw new NoSuchClusterException(s"$bkName doesn't exist"))
              }
              .getOrElse {
                val bkClusters = clusters.keys.filter(_.isInstanceOf[BrokerClusterInfo])
                bkClusters.size match {
                  case 0 =>
                    throw new IllegalArgumentException(
                      s"You didn't specify the bk cluster for wk cluster:${req.name}, and there is no default bk cluster")
                  case 1 => bkClusters.head.name
                  case _ =>
                    throw new IllegalArgumentException(
                      s"You didn't specify the bk cluster for wk cluster ${req.name}, and there are too many bk clusters:{${bkClusters
                        .map(_.name)}}")
                }
              }
          }
          .flatMap(bkName => jarStore.urls(req.jars).map(bkName -> _))
          .flatMap {
            case (bkName, urls) =>
              clusterCollie
                .workerCollie()
                .creator()
                .clusterName(req.name)
                .clientPort(req.clientPort)
                .brokerClusterName(bkName)
                .groupId(req.groupId)
                .configTopicName(req.configTopicName)
                .configTopicReplications(req.configTopicReplications)
                .offsetTopicName(req.offsetTopicName)
                .offsetTopicPartitions(req.offsetTopicPartitions)
                .offsetTopicReplications(req.offsetTopicReplications)
                .statusTopicName(req.statusTopicName)
                .statusTopicPartitions(req.statusTopicPartitions)
                .statusTopicReplications(req.statusTopicReplications)
                .imageName(req.imageName)
                .jarUrls(urls)
                .nodeNames(req.nodeNames)
                .create()
        }
    )
}
