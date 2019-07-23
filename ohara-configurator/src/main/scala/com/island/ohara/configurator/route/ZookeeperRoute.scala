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
import com.island.ohara.agent.{ClusterCollie, NodeCollie}
import com.island.ohara.client.configurator.v0.DataKey
import com.island.ohara.client.configurator.v0.ZookeeperApi._
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.route.RouteUtils.{
  HookBeforeDelete,
  HookOfCreation,
  HookOfGet,
  HookOfList,
  HookOfUpdate
}
import com.island.ohara.configurator.store.DataStore

import scala.concurrent.{ExecutionContext, Future}

object ZookeeperRoute {

  private[this] def updateState(info: ZookeeperClusterInfo)(
    implicit store: DataStore,
    clusterCollie: ClusterCollie,
    executionContext: ExecutionContext): Future[ZookeeperClusterInfo] =
    store
      .value[ZookeeperClusterInfo](info.name)
      .flatMap(
        cluster =>
          clusterCollie.zookeeperCollie
            .exist(cluster.name)
            .flatMap {
              if (_) {
                clusterCollie.zookeeperCollie.cluster(cluster.name).map(_._1)
              } else {
                // if zookeeper cluster was not created, we initial state and error
                Future.successful(cluster.copy(state = None, error = None))
              }
            }
            .flatMap { finalData =>
              store.addIfPresent[ZookeeperClusterInfo](
                name = info.name,
                updater = (previous: ZookeeperClusterInfo) =>
                  previous.copy(
                    state = finalData.state,
                    nodeNames = finalData.nodeNames,
                    deadNodes = finalData.deadNodes,
                )
              )
          }
      )

  private[this] def hookOfGet(implicit store: DataStore,
                              clusterCollie: ClusterCollie,
                              executionContext: ExecutionContext): HookOfGet[ZookeeperClusterInfo] = updateState(_)

  private[this] def hookOfList(implicit store: DataStore,
                               clusterCollie: ClusterCollie,
                               executionContext: ExecutionContext): HookOfList[ZookeeperClusterInfo] =
    Future.traverse(_)(updateState)

  private[this] def hookOfCreation: HookOfCreation[Creation, ZookeeperClusterInfo] = (_: String, creation: Creation) =>
    Future.successful(
      ZookeeperClusterInfo(
        name = creation.name,
        imageName = creation.imageName,
        clientPort = creation.clientPort,
        peerPort = creation.peerPort,
        electionPort = creation.electionPort,
        nodeNames = creation.nodeNames,
        deadNodes = Set.empty,
        tags = creation.tags,
        state = None,
        error = None,
        lastModified = CommonUtils.current()
      ))

  private[this] def hookOfUpdate(
    implicit executionContext: ExecutionContext): HookOfUpdate[Creation, Update, ZookeeperClusterInfo] =
    (key: DataKey, update: Update, previous: Option[ZookeeperClusterInfo]) =>
      Future
        .successful(
          previous.fold(ZookeeperClusterInfo(
            name = key.name,
            imageName = update.imageName.getOrElse(IMAGE_NAME_DEFAULT),
            clientPort = update.clientPort.getOrElse(CommonUtils.availablePort()),
            peerPort = update.peerPort.getOrElse(CommonUtils.availablePort()),
            electionPort = update.electionPort.getOrElse(CommonUtils.availablePort()),
            nodeNames = update.nodeNames.getOrElse(Set.empty),
            deadNodes = Set.empty,
            tags = update.tags.getOrElse(Map.empty),
            state = None,
            error = None,
            lastModified = CommonUtils.current()
          )) { previous =>
            previous.copy(
              imageName = update.imageName.getOrElse(previous.imageName),
              clientPort = update.clientPort.getOrElse(previous.clientPort),
              peerPort = update.peerPort.getOrElse(previous.peerPort),
              electionPort = update.electionPort.getOrElse(previous.electionPort),
              nodeNames = update.nodeNames.getOrElse(previous.nodeNames),
              tags = update.tags.getOrElse(previous.tags),
              lastModified = CommonUtils.current()
            )
          })
        .map { zookeeperClusterInfo =>
          if (zookeeperClusterInfo.state.isDefined)
            throw new RuntimeException(s"You cannot update property on non-stopped zookeeper cluster: $key")
          zookeeperClusterInfo
      }

  private[this] def hookBeforeDelete(implicit store: DataStore,
                                     clusterCollie: ClusterCollie,
                                     executionContext: ExecutionContext): HookBeforeDelete = (key: DataKey) =>
    store.get[ZookeeperClusterInfo](key).flatMap {
      _.fold(Future.unit) { info =>
        updateState(info).flatMap { data =>
          if (data.state.isEmpty) Future.unit
          else Future.failed(new RuntimeException(s"You cannot delete a non-stopped zookeeper :$key"))
        }
      }
  }

  def apply(implicit store: DataStore,
            clusterCollie: ClusterCollie,
            nodeCollie: NodeCollie,
            executionContext: ExecutionContext): server.Route =
    RouteUtils.basicRoute(
      root = ZOOKEEPER_PREFIX_PATH,
      enableGroup = false,
      hookOfCreation = hookOfCreation,
      hookOfUpdate = hookOfUpdate,
      hookOfGet = hookOfGet,
      hookOfList = hookOfList,
      hookBeforeDelete = hookBeforeDelete
    ) ~
      RouteUtils.appendRouteOfClusterAction(
        collie = clusterCollie.zookeeperCollie,
        root = ZOOKEEPER_PREFIX_PATH,
        enableGroup = false,
        hookOfStart = (_, req: ZookeeperClusterInfo) =>
          clusterCollie.zookeeperCollie.creator
            .clusterName(req.name)
            .clientPort(req.clientPort)
            .electionPort(req.electionPort)
            .peerPort(req.peerPort)
            .imageName(req.imageName)
            .nodeNames(req.nodeNames)
            .threadPool(executionContext)
            .create(),
        hookOfStop = (name: String) =>
          clusterCollie.brokerCollie
            .clusters()
            .flatMap(
              _.keys
                .find(_.zookeeperClusterName == name)
                .fold(Future.successful(name))(c =>
                  Future.failed(new IllegalArgumentException(
                    s"you can't remove zookeeper cluster:$name since it is used by broker cluster:${c.name}")))
          )
      )
}
