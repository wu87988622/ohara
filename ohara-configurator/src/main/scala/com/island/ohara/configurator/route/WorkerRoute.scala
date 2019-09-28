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
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.file.FileStore
import com.island.ohara.configurator.route.hook.{HookOfAction, HookOfCreation, HookOfUpdating}
import com.island.ohara.configurator.store.{DataStore, MeterCache}

import scala.concurrent.{ExecutionContext, Future}
object WorkerRoute {

  private[this] def hookOfCreation(implicit fileStore: FileStore,
                                   brokerCollie: BrokerCollie,
                                   executionContext: ExecutionContext): HookOfCreation[Creation, WorkerClusterInfo] =
    (creation: Creation) =>
      creation.brokerClusterKey.map(Future.successful).getOrElse(CollieUtils.singleBrokerCluster()).flatMap { bkKey =>
        Future
          .traverse(creation.jarKeys)(fileStore.fileInfo)
          .map(_.toSeq)
          .map(jarInfos =>
            WorkerClusterInfo(
              settings = WorkerApi.access.request
                .settings(creation.settings)
                .brokerClusterKey(bkKey)
                .jarInfos(jarInfos)
                .creation
                .settings,
              connectors = Seq.empty,
              aliveNodes = Set.empty,
              state = None,
              error = None,
              lastModified = CommonUtils.current()
          ))
    }

  private[this] def HookOfUpdating(
    implicit fileStore: FileStore,
    clusterCollie: ClusterCollie,
    brokerCollie: BrokerCollie,
    executionContext: ExecutionContext): HookOfUpdating[Creation, Updating, WorkerClusterInfo] =
    (key: ObjectKey, update: Updating, previousOption: Option[WorkerClusterInfo]) =>
      clusterCollie.workerCollie
        .clusters()
        .flatMap { clusters =>
          if (clusters.keys.filter(_.key == key).exists(_.state.nonEmpty))
            throw new RuntimeException(s"You cannot update property on non-stopped worker cluster: $key")
          update.brokerClusterKey
            .orElse(previousOption.map(_.brokerClusterKey))
            .map(Future.successful)
            .getOrElse(CollieUtils.singleBrokerCluster())
        }
        .flatMap { bkKey =>
          // use PUT as creation request
          Future.traverse(update.jarKeys.getOrElse(Set.empty))(fileStore.fileInfo).map(_.toSeq).map { jarInfos =>
            // 1) fill the previous settings (if exists)
            // 2) overwrite previous settings by updated settings
            // 3) fill the ignored settings by creation
            val newSettings = previousOption.map(_.settings).getOrElse(Map.empty) ++ update.settings
            WorkerClusterInfo(
              settings = WorkerApi.access.request
                .settings(newSettings)
                .brokerClusterKey(bkKey)
                .jarInfos(jarInfos)
                .creation
                .settings,
              connectors = Seq.empty,
              aliveNodes = Set.empty,
              state = None,
              error = None,
              lastModified = CommonUtils.current()
            )
          }
      }

  private[this] def hookOfStart(implicit store: DataStore,
                                clusterCollie: ClusterCollie,
                                executionContext: ExecutionContext): HookOfAction =
    (key: ObjectKey, _, _) =>
      store
        .value[WorkerClusterInfo](key)
        .flatMap { data =>
          clusterCollie.clusters().map(_.keys.toSeq).map(_ -> data)
        }
        .flatMap {
          case (clusters, workerClusterInfo) =>
            val wkClusters = clusters.filter(_.isInstanceOf[WorkerClusterInfo]).map(_.asInstanceOf[WorkerClusterInfo])

            // check broker cluster
            if (!clusters.filter(_.isInstanceOf[BrokerClusterInfo]).exists(_.key == workerClusterInfo.brokerClusterKey))
              throw new NoSuchClusterException(s"broker cluster:${workerClusterInfo.brokerClusterKey} doesn't exist")

            // check group id
            wkClusters.find(_.groupId == workerClusterInfo.groupId).foreach { cluster =>
              throw new IllegalArgumentException(
                s"group id:${workerClusterInfo.groupId} is used by wk cluster:${cluster.name}")
            }

            // check setting topic
            wkClusters.find(_.configTopicName == workerClusterInfo.configTopicName).foreach { cluster =>
              throw new IllegalArgumentException(
                s"configTopicName:${workerClusterInfo.configTopicName} is used by wk cluster:${cluster.name}")
            }

            // check offset topic
            wkClusters.find(_.offsetTopicName == workerClusterInfo.offsetTopicName).foreach { cluster =>
              throw new IllegalArgumentException(
                s"offsetTopicName:${workerClusterInfo.offsetTopicName} is used by wk cluster:${cluster.name}")
            }

            // check status topic
            wkClusters.find(_.statusTopicName == workerClusterInfo.statusTopicName).foreach { cluster =>
              throw new IllegalArgumentException(
                s"statusTopicName:${workerClusterInfo.statusTopicName} is used by wk cluster:${cluster.name}")
            }

            clusterCollie.workerCollie.creator
              .settings(workerClusterInfo.settings)
              .name(workerClusterInfo.name)
              .group(workerClusterInfo.group)
              .clientPort(workerClusterInfo.clientPort)
              .jmxPort(workerClusterInfo.jmxPort)
              .brokerClusterKey(workerClusterInfo.brokerClusterKey)
              .groupId(workerClusterInfo.groupId)
              .configTopicName(workerClusterInfo.configTopicName)
              .configTopicReplications(workerClusterInfo.configTopicReplications)
              .offsetTopicName(workerClusterInfo.offsetTopicName)
              .offsetTopicPartitions(workerClusterInfo.offsetTopicPartitions)
              .offsetTopicReplications(workerClusterInfo.offsetTopicReplications)
              .statusTopicName(workerClusterInfo.statusTopicName)
              .statusTopicPartitions(workerClusterInfo.statusTopicPartitions)
              .statusTopicReplications(workerClusterInfo.statusTopicReplications)
              .imageName(workerClusterInfo.imageName)
              .jarInfos(workerClusterInfo.jarInfos)
              .nodeNames(workerClusterInfo.nodeNames)
              .threadPool(executionContext)
              .create()
        }
        .map(_ => Unit)

  private[this] def hookBeforeStop: HookOfAction = (_, _, _) => Future.unit

  def apply(implicit store: DataStore,
            meterCache: MeterCache,
            brokerCollie: BrokerCollie,
            workerCollie: WorkerCollie,
            clusterCollie: ClusterCollie,
            nodeCollie: NodeCollie,
            fileStore: FileStore,
            executionContext: ExecutionContext): server.Route =
    clusterRoute(
      root = WORKER_PREFIX_PATH,
      metricsKey = None,
      hookOfCreation = hookOfCreation,
      HookOfUpdating = HookOfUpdating,
      hookOfStart = hookOfStart,
      hookBeforeStop = hookBeforeStop
    )
}
