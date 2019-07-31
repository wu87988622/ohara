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
import akka.http.scaladsl.server.Directives._
import com.island.ohara.agent.{ClusterCollie, NoSuchClusterException, NodeCollie}
import com.island.ohara.client.configurator.v0.BrokerApi.{Creation, _}
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.route.RouteUtils._
import com.island.ohara.configurator.store.DataStore
import com.island.ohara.kafka.connector.json.ObjectKey

import scala.concurrent.{ExecutionContext, Future}
object BrokerRoute {

  //TODO : move this to RouteUtils after #1544...by Sam
  private[this] def updateState(info: BrokerClusterInfo)(
    implicit store: DataStore,
    clusterCollie: ClusterCollie,
    executionContext: ExecutionContext): Future[BrokerClusterInfo] =
    store
      .value[BrokerClusterInfo](info.key)
      .flatMap(
        cluster =>
          clusterCollie.brokerCollie
            .exist(cluster.name)
            .flatMap {
              if (_) {
                clusterCollie.brokerCollie.cluster(cluster.name).map(_._1)
              } else {
                // if broker cluster was not created, we initial state and error
                Future.successful(cluster.copy(state = None, error = None))
              }
            }
            .flatMap { finalData =>
              store.addIfPresent[BrokerClusterInfo](
                key = info.key,
                updater = (previous: BrokerClusterInfo) =>
                  previous.copy(
                    zookeeperClusterName = finalData.zookeeperClusterName,
                    state = finalData.state,
                    nodeNames = finalData.nodeNames,
                    deadNodes = finalData.deadNodes,
                )
              )
          }
      )

  private[this] def hookOfGet(implicit store: DataStore,
                              clusterCollie: ClusterCollie,
                              executionContext: ExecutionContext): HookOfGet[BrokerClusterInfo] =
    updateState(_)

  private[this] def hookOfList(implicit store: DataStore,
                               clusterCollie: ClusterCollie,
                               executionContext: ExecutionContext): HookOfList[BrokerClusterInfo] =
    Future.traverse(_)(updateState)

  private[this] def hookOfCreation: HookOfCreation[Creation, BrokerClusterInfo] = (creation: Creation) =>
    Future.successful(
      BrokerClusterInfo(
        name = creation.name,
        imageName = creation.imageName,
        zookeeperClusterName = creation.zookeeperClusterName,
        clientPort = creation.clientPort,
        exporterPort = creation.exporterPort,
        jmxPort = creation.jmxPort,
        nodeNames = creation.nodeNames,
        deadNodes = Set.empty,
        tags = creation.tags,
        state = None,
        error = None,
        lastModified = CommonUtils.current()
      )
  )

  private[this] def hookOfUpdate(
    implicit executionContext: ExecutionContext): HookOfUpdate[Creation, Update, BrokerClusterInfo] =
    (key: ObjectKey, update: Update, previous: Option[BrokerClusterInfo]) =>
      Future
        .successful(
          previous.fold(BrokerClusterInfo(
            name = key.name,
            imageName = update.imageName.getOrElse(IMAGE_NAME_DEFAULT),
            zookeeperClusterName = update.zookeeperClusterName,
            clientPort = update.clientPort.getOrElse(CommonUtils.availablePort()),
            exporterPort = update.exporterPort.getOrElse(CommonUtils.availablePort()),
            jmxPort = update.jmxPort.getOrElse(CommonUtils.availablePort()),
            nodeNames = update.nodeNames.getOrElse(Set.empty),
            deadNodes = Set.empty,
            tags = update.tags.getOrElse(Map.empty),
            state = None,
            error = None,
            lastModified = CommonUtils.current()
          )) { previous =>
            previous.copy(
              imageName = update.imageName.getOrElse(previous.imageName),
              zookeeperClusterName = update.zookeeperClusterName,
              clientPort = update.clientPort.getOrElse(previous.clientPort),
              exporterPort = update.exporterPort.getOrElse(previous.exporterPort),
              jmxPort = update.jmxPort.getOrElse(previous.jmxPort),
              nodeNames = update.nodeNames.getOrElse(previous.nodeNames),
              tags = update.tags.getOrElse(previous.tags),
              lastModified = CommonUtils.current()
            )
          })
        .map { brokerClusterInfo =>
          if (brokerClusterInfo.state.isDefined)
            throw new RuntimeException(s"You cannot update property on non-stopped broker cluster: $key")
          brokerClusterInfo
      }

  private[this] def hookBeforeDelete(implicit store: DataStore,
                                     clusterCollie: ClusterCollie,
                                     executionContext: ExecutionContext): HookBeforeDelete = (key: ObjectKey) =>
    store.get[BrokerClusterInfo](key).flatMap {
      _.fold(Future.unit) { info =>
        updateState(info).flatMap { data =>
          if (data.state.isEmpty) Future.unit
          else Future.failed(new RuntimeException(s"You cannot delete a non-stopped broker :$key"))
        }
      }
  }

  private[this] def hookOfGroup: HookOfGroup = _ => GROUP_DEFAULT

  def apply(implicit store: DataStore,
            clusterCollie: ClusterCollie,
            nodeCollie: NodeCollie,
            executionContext: ExecutionContext): server.Route =
    RouteUtils.route(
      root = BROKER_PREFIX_PATH,
      hookOfGroup = hookOfGroup,
      hookOfCreation = hookOfCreation,
      hookOfUpdate = hookOfUpdate,
      hookOfGet = hookOfGet,
      hookOfList = hookOfList,
      hookBeforeDelete = hookBeforeDelete
    ) ~
      RouteUtils.appendRouteOfClusterAction(
        collie = clusterCollie.brokerCollie,
        root = BROKER_PREFIX_PATH,
        hookOfGroup = hookOfGroup,
        hookOfStart = (clusters, req: BrokerClusterInfo) => {
          val zkName = req.zookeeperClusterName
            .map { zkName =>
              clusters
                .filter(_.isInstanceOf[ZookeeperClusterInfo])
                .find(_.name == zkName)
                .map(_.name)
                .getOrElse(throw new NoSuchClusterException(s"zookeeper cluster:$zkName doesn't exist"))
            }
            .getOrElse {
              val zkClusters = clusters.filter(_.isInstanceOf[ZookeeperClusterInfo])
              zkClusters.size match {
                case 0 =>
                  throw new IllegalArgumentException(
                    s"You didn't specify the zk cluster for bk cluster:${req.name}, and there is no default zk cluster")
                case 1 => zkClusters.head.name
                case _ =>
                  throw new IllegalArgumentException(
                    s"You didn't specify the zk cluster for bk cluster ${req.name}, and there are too many zk clusters:{${zkClusters
                      .map(_.name)}}")
              }
            }
          val sameZkNameClusters = clusters
            .filter(_.isInstanceOf[BrokerClusterInfo])
            .map(_.asInstanceOf[BrokerClusterInfo])
            .filter(_.zookeeperClusterName.exists(_ == zkName))
          if (sameZkNameClusters.nonEmpty)
            throw new IllegalArgumentException(
              s"zk cluster:$zkName is already used by broker cluster:${sameZkNameClusters.head.name}")
          clusterCollie.brokerCollie.creator
            .clusterName(req.name)
            .clientPort(req.clientPort)
            .exporterPort(req.exporterPort)
            .jmxPort(req.jmxPort)
            .zookeeperClusterName(zkName)
            .imageName(req.imageName)
            .nodeNames(req.nodeNames)
            .threadPool(executionContext)
            .create()
        },
        hookOfStop = (name: String) =>
          clusterCollie.workerCollie
            .clusters()
            .flatMap(
              _.keys
                .find(_.brokerClusterName == name)
                .fold(Future.successful(name))(c =>
                  Future.failed(new IllegalArgumentException(
                    s"you can't remove broker cluster:$name since it is used by worker cluster:${c.name}")))
          )
      )
}
