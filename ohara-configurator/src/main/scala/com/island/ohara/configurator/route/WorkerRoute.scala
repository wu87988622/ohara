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

import scala.concurrent.{ExecutionContext, Future}
object WorkerRoute {

  def apply(implicit clusterCollie: ClusterCollie,
            nodeCollie: NodeCollie,
            jarStore: JarStore,
            executionContext: ExecutionContext): server.Route =
    RouteUtils.basicRouteOfCluster(
      collie = clusterCollie.workerCollie(),
      root = WORKER_PREFIX_PATH,
      hookBeforeDelete = (_, name) => Future.successful(name),
      hookOfCreation = (clusters, req: Creation) =>
        Future
          .traverse(req.jarKeys.map(jarKey => (jarKey.group, jarKey.name))) {
            case (group, id) => jarStore.jarInfo(group, id)
          }
          .map { jarInfos =>
            val wkClusters = clusters.filter(_.isInstanceOf[WorkerClusterInfo]).map(_.asInstanceOf[WorkerClusterInfo])

            // check group id
            wkClusters.find(_.groupId == req.groupId).foreach { cluster =>
              throw new IllegalArgumentException(s"group id:${req.groupId} is used by wk cluster:${cluster.name}")
            }

            // check setting topic
            wkClusters.find(_.configTopicName == req.configTopicName).foreach { cluster =>
              throw new IllegalArgumentException(
                s"configTopicName:${req.configTopicName} is used by wk cluster:${cluster.name}")
            }

            // check offset topic
            wkClusters.find(_.offsetTopicName == req.offsetTopicName).foreach { cluster =>
              throw new IllegalArgumentException(
                s"offsetTopicName:${req.offsetTopicName} is used by wk cluster:${cluster.name}")
            }

            // check status topic
            wkClusters.find(_.statusTopicName == req.statusTopicName).foreach { cluster =>
              throw new IllegalArgumentException(
                s"statusTopicName:${req.statusTopicName} is used by wk cluster:${cluster.name}")
            }
            jarInfos
          }
          // match the broker cluster
          .map(req.brokerClusterName
            .map { bkName =>
              clusters
                .filter(_.isInstanceOf[BrokerClusterInfo])
                .find(_.name == bkName)
                .map(_.name)
                .getOrElse(throw new NoSuchClusterException(s"broker cluster:$bkName doesn't exist"))
            }
            .getOrElse {
              val bkClusters = clusters.filter(_.isInstanceOf[BrokerClusterInfo])
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
            } -> _)
          .flatMap {
            case (bkName, jarInfos) =>
              clusterCollie
                .workerCollie()
                .creator()
                .clusterName(req.name)
                .clientPort(req.clientPort)
                .jmxPort(req.jmxPort)
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
                .jarInfos(jarInfos.toSeq)
                .nodeNames(req.nodeNames)
                .threadPool(executionContext)
                .create()
        }
    )
}
