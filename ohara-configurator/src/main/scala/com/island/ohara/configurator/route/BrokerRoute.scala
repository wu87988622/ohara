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
import com.island.ohara.agent.{BrokerCollie, ClusterCollie, NodeCollie, ZookeeperCollie}
import com.island.ohara.client.configurator.v0.BrokerApi.{Creation, _}
import com.island.ohara.client.configurator.v0.{BrokerApi, TopicApi}
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.route.hook.{HookOfAction, HookOfCreation, HookOfGroup, HookOfUpdate}
import com.island.ohara.configurator.store.{DataStore, MeterCache}

import scala.concurrent.{ExecutionContext, Future}
object BrokerRoute {

  private[this] def hookOfCreation(implicit zookeeperCollie: ZookeeperCollie,
                                   executionContext: ExecutionContext): HookOfCreation[Creation, BrokerClusterInfo] =
    (creation: Creation) =>
      creation.zookeeperClusterName.map(Future.successful).getOrElse(CollieUtils.singleCluster()).map { zkName =>
        BrokerClusterInfo(
          settings = BrokerApi.access.request.settings(creation.settings).zookeeperClusterName(zkName).creation.settings,
          deadNodes = Set.empty,
          state = None,
          error = None,
          lastModified = CommonUtils.current(),
          topicSettingDefinitions = TopicApi.TOPIC_DEFINITIONS
        )
    }

  private[this] def hookOfUpdate(
    implicit zookeeperCollie: ZookeeperCollie,
    clusterCollie: ClusterCollie,
    executionContext: ExecutionContext): HookOfUpdate[Creation, Update, BrokerClusterInfo] =
    (key: ObjectKey, update: Update, previousOption: Option[BrokerClusterInfo]) =>
      clusterCollie.brokerCollie
        .clusters()
        .flatMap { clusters =>
          if (clusters.keys.filter(_.name == key.name()).exists(_.state.nonEmpty))
            throw new RuntimeException(s"You cannot update property on non-stopped broker cluster: $key")
          update.zookeeperClusterName
            .orElse(previousOption.map(_.zookeeperClusterName))
            .map(Future.successful)
            .getOrElse(CollieUtils.singleCluster())
        }
        .map { zkName =>
          val newSettings = previousOption.map(_.settings).getOrElse(Map.empty) ++ update.settings
          BrokerClusterInfo(
            settings = BrokerApi.access.request.settings(newSettings).zookeeperClusterName(zkName).creation.settings,
            deadNodes = Set.empty,
            state = None,
            error = None,
            lastModified = CommonUtils.current(),
            topicSettingDefinitions = TopicApi.TOPIC_DEFINITIONS
          )
      }

  private[this] def hookOfStart(implicit store: DataStore,
                                clusterCollie: ClusterCollie,
                                executionContext: ExecutionContext): HookOfAction =
    (key: ObjectKey, _: String, _: Map[String, String]) =>
      store
        .value[BrokerClusterInfo](key)
        .flatMap(brokerClusterInfo => clusterCollie.clusters().map(_.keys.toSeq).map(_ -> brokerClusterInfo))
        .flatMap {
          case (clusters, brokerClusterInfo) =>
            val sameZkNameClusters = clusters
              .filter(_.isInstanceOf[BrokerClusterInfo])
              .map(_.asInstanceOf[BrokerClusterInfo])
              .filter(_.zookeeperClusterName == brokerClusterInfo.zookeeperClusterName)
            if (sameZkNameClusters.nonEmpty)
              throw new IllegalArgumentException(
                s"zk cluster:${brokerClusterInfo.zookeeperClusterName} is already used by broker cluster:${sameZkNameClusters.head.name}")
            clusterCollie.brokerCollie.creator
              .clusterName(brokerClusterInfo.name)
              .clientPort(brokerClusterInfo.clientPort)
              .exporterPort(brokerClusterInfo.exporterPort)
              .jmxPort(brokerClusterInfo.jmxPort)
              .zookeeperClusterName(brokerClusterInfo.zookeeperClusterName)
              .imageName(brokerClusterInfo.imageName)
              .nodeNames(brokerClusterInfo.nodeNames)
              .threadPool(executionContext)
              .create()
        }
        .map(_ => Unit)

  private[this] def hookBeforeStop(implicit store: DataStore,
                                   clusterCollie: ClusterCollie,
                                   executionContext: ExecutionContext): HookOfAction =
    (key: ObjectKey, _: String, _: Map[String, String]) =>
      store
        .value[BrokerClusterInfo](key)
        .flatMap(
          brokerClusterInfo =>
            clusterCollie.workerCollie
              .clusters()
              .map(
                _.keys
                  .find(_.brokerClusterName == brokerClusterInfo.name)
                  .map(cluster =>
                    throw new IllegalArgumentException(
                      s"you can't remove broker cluster:${brokerClusterInfo.name} since it is used by worker cluster:${cluster.name}"))
            ))

  private[this] def hookOfGroup: HookOfGroup = _ => BROKER_GROUP_DEFAULT

  def apply(implicit store: DataStore,
            meterCache: MeterCache,
            zookeeperCollie: ZookeeperCollie,
            brokerCollie: BrokerCollie,
            clusterCollie: ClusterCollie,
            nodeCollie: NodeCollie,
            executionContext: ExecutionContext): server.Route =
    clusterRoute(
      root = BROKER_PREFIX_PATH,
      metricsKey = None,
      hookOfGroup = hookOfGroup,
      hookOfCreation = hookOfCreation,
      hookOfUpdate = hookOfUpdate,
      hookOfStart = hookOfStart,
      hookBeforeStop = hookBeforeStop
    )
}
