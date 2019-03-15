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
import com.island.ohara.client.configurator.v0.WorkerApi
import com.island.ohara.client.configurator.v0.WorkerApi._
import com.island.ohara.common.util.CommonUtils
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
      defaultImage = WorkerApi.IMAGE_NAME_DEFAULT,
      hookBeforeDelete = (_, name) => Future.successful(name),
      hookOfCreation = (clusters, req: WorkerClusterCreationRequest) =>
        jarStore
          .urls(req.jars)
          .map { jars =>
            val wkClusters = clusters.filter(_.isInstanceOf[WorkerClusterInfo]).map(_.asInstanceOf[WorkerClusterInfo])

            // check group id
            req.groupId.foreach(groupId =>
              wkClusters
                .find(_.groupId == groupId)
                .foreach(c => throw new IllegalArgumentException(s"group id:$groupId is used by wk cluster:${c.name}")))

            // check config topic
            req.configTopicName.foreach(configTopicName =>
              wkClusters
                .find(_.configTopicName == configTopicName)
                .foreach(c =>
                  throw new IllegalArgumentException(s"config topic:$configTopicName is used by wk cluster:${c.name}")))

            // check offset topic
            req.offsetTopicName.foreach(offsetTopicName =>
              wkClusters
                .find(_.offsetTopicName == offsetTopicName)
                .foreach(c =>
                  throw new IllegalArgumentException(s"offset topic:$offsetTopicName is used by wk cluster:${c.name}")))

            // check status topic
            req.statusTopicName.foreach(statusTopicName =>
              wkClusters
                .find(_.statusTopicName == statusTopicName)
                .foreach(c =>
                  throw new IllegalArgumentException(s"status topic$statusTopicName is used by wk cluster:${c.name}")))

            jars
          }
          // match the broker cluster
          .map(req.brokerClusterName
            .map { bkName =>
              clusters
                .filter(_.isInstanceOf[BrokerClusterInfo])
                .find(_.name == bkName)
                .map(_.name)
                .getOrElse(throw new NoSuchClusterException(s"$bkName doesn't exist"))
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
            case (bkName, urls) =>
              clusterCollie
                .workerCollie()
                .creator()
                .clusterName(req.name)
                .clientPort(req.clientPort.getOrElse(WorkerApi.CLIENT_PORT_DEFAULT))
                .brokerClusterName(bkName)
                .groupId(req.groupId.getOrElse(CommonUtils.randomString(10)))
                .configTopicName(req.configTopicName.getOrElse(s"config-${CommonUtils.randomString(10)}"))
                .configTopicReplications(req.configTopicReplications.getOrElse(
                  WorkerApi.CONFIG_TOPIC_REPLICATIONS_DEFAULT))
                .offsetTopicName(req.offsetTopicName.getOrElse(s"offset-${CommonUtils.randomString(10)}"))
                .offsetTopicPartitions(req.offsetTopicPartitions.getOrElse(WorkerApi.OFFSET_TOPIC_PARTITIONS_DEFAULT))
                .offsetTopicReplications(req.offsetTopicReplications.getOrElse(
                  WorkerApi.OFFSET_TOPIC_REPLICATIONS_DEFAULT))
                .statusTopicName(req.statusTopicName.getOrElse(s"status-${CommonUtils.randomString(10)}"))
                .statusTopicPartitions(req.statusTopicPartitions.getOrElse(WorkerApi.STATUS_TOPIC_PARTITIONS_DEFAULT))
                .statusTopicReplications(req.statusTopicReplications.getOrElse(
                  WorkerApi.STATUS_TOPIC_REPLICATIONS_DEFAULT))
                .imageName(req.imageName.getOrElse(WorkerApi.IMAGE_NAME_DEFAULT))
                .jarUrls(urls)
                .nodeNames(req.nodeNames)
                .create
        }
    )
}
