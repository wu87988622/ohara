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
import com.island.ohara.client.configurator.v0.BrokerApi.{Creation, _}
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo
import com.island.ohara.client.configurator.v0.{BrokerApi, TopicApi}
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.route.hook.{HookOfAction, HookOfCreation, HookOfUpdating}
import com.island.ohara.configurator.store.{DataStore, MeterCache}

import scala.concurrent.{ExecutionContext, Future}
object BrokerRoute {

  private[this] def creationToClusterInfo(
    creation: Creation)(implicit store: DataStore, executionContext: ExecutionContext): Future[BrokerClusterInfo] =
    store.exist[ZookeeperClusterInfo](creation.zookeeperClusterKey).map {
      if (_)
        BrokerClusterInfo(
          settings = creation.settings,
          aliveNodes = Set.empty,
          state = None,
          error = None,
          lastModified = CommonUtils.current(),
          topicSettingDefinitions = TopicApi.TOPIC_DEFINITIONS
        )
      else throw new IllegalArgumentException(s"zookeeper cluster:${creation.zookeeperClusterKey} does not exist")
    }

  private[this] def hookOfCreation(implicit store: DataStore,
                                   executionContext: ExecutionContext): HookOfCreation[Creation, BrokerClusterInfo] =
    creationToClusterInfo(_)

  private[this] def hookOfUpdating(implicit store: DataStore,
                                   brokerCollie: BrokerCollie,
                                   executionContext: ExecutionContext): HookOfUpdating[Updating, BrokerClusterInfo] =
    (key: ObjectKey, updating: Updating, previousOption: Option[BrokerClusterInfo]) =>
      if (previousOption.isEmpty) creationToClusterInfo(BrokerApi.access.request.settings(updating.settings).creation)
      else {
        brokerCollie.exist(key).flatMap {
          if (_) throw new IllegalArgumentException(s"You cannot update property on non-stopped broker cluster: $key")
          else // 1) fill the previous settings (if exists)
            // 2) overwrite previous settings by updated settings
            // 3) fill the ignored settings by creation
            creationToClusterInfo(
              BrokerApi.access.request
                .settings(previousOption.map(_.settings).getOrElse(Map.empty))
                .settings(updating.settings)
                // the key is not in update's settings so we have to add it to settings
                .key(key)
                .creation)
        }
    }

  private[this] def hookOfStart(implicit store: DataStore,
                                meterCache: MeterCache,
                                zookeeperCollie: ZookeeperCollie,
                                brokerCollie: BrokerCollie,
                                serviceCollie: ServiceCollie,
                                executionContext: ExecutionContext): HookOfAction =
    (key: ObjectKey, _, _) =>
      (for {
        bkInfo <- store.value[BrokerClusterInfo](key)
        zks <- runningZookeeperClusters()
        bks <- runningBrokerClusters()
      } yield (bkInfo, zks, bks))
        .flatMap {
          case (brokerClusterInfo, runningZookeeperClusters, runningBrokerClusters) =>
            val conflictBrokerClusters =
              runningBrokerClusters.filter(_.zookeeperClusterKey == brokerClusterInfo.zookeeperClusterKey)
            if (conflictBrokerClusters.nonEmpty)
              throw new IllegalArgumentException(
                s"zk cluster:${brokerClusterInfo.zookeeperClusterKey} is already used by broker cluster:${conflictBrokerClusters.head.name}")
            if (!runningZookeeperClusters.exists(_.key == brokerClusterInfo.zookeeperClusterKey))
              throw new IllegalArgumentException(s"zk cluster:${brokerClusterInfo.zookeeperClusterKey} is not running")
            serviceCollie.brokerCollie.creator
              .settings(brokerClusterInfo.settings)
              .name(brokerClusterInfo.name)
              .group(brokerClusterInfo.group)
              .clientPort(brokerClusterInfo.clientPort)
              .exporterPort(brokerClusterInfo.exporterPort)
              .jmxPort(brokerClusterInfo.jmxPort)
              .zookeeperClusterKey(brokerClusterInfo.zookeeperClusterKey)
              .imageName(brokerClusterInfo.imageName)
              .nodeNames(brokerClusterInfo.nodeNames)
              .threadPool(executionContext)
              .create()
        }
        .map(_ => Unit)

  private[this] def hookBeforeStop(implicit store: DataStore,
                                   meterCache: MeterCache,
                                   workerCollie: WorkerCollie,
                                   streamCollie: StreamCollie,
                                   executionContext: ExecutionContext): HookOfAction =
    (key: ObjectKey, _: String, _: Map[String, String]) =>
      (for {
        bkInfo <- store.value[BrokerClusterInfo](key)
        wks <- runningWorkerClusters()
        sts <- runningStreamClusters()
      } yield (bkInfo, wks, sts)).map {
        case (brokerClusterInfo, runningWorkerClusters, runningStreamClusters) =>
          val conflictWks = runningWorkerClusters.filter(_.brokerClusterKey == brokerClusterInfo.key)
          if (conflictWks.nonEmpty)
            throw new IllegalArgumentException(
              s"you can't remove broker cluster:$key since it is used by worker cluster:${conflictWks.mkString(",")}")
          val conflictStreams = runningStreamClusters.filter(_.brokerClusterKey == brokerClusterInfo.key)
          if (conflictStreams.nonEmpty)
            throw new IllegalArgumentException(
              s"you can't remove broker cluster:$key since it is used by stream cluster:${conflictStreams.mkString(",")}")
    }

  def apply(implicit store: DataStore,
            meterCache: MeterCache,
            zookeeperCollie: ZookeeperCollie,
            brokerCollie: BrokerCollie,
            workerCollie: WorkerCollie,
            streamCollie: StreamCollie,
            serviceCollie: ServiceCollie,
            dataCollie: DataCollie,
            executionContext: ExecutionContext): server.Route =
    clusterRoute[BrokerClusterInfo, BrokerClusterStatus, Creation, Updating](
      root = BROKER_PREFIX_PATH,
      metricsKey = None,
      hookOfCreation = hookOfCreation,
      hookOfUpdating = hookOfUpdating,
      hookOfStart = hookOfStart,
      hookBeforeStop = hookBeforeStop
    )
}
