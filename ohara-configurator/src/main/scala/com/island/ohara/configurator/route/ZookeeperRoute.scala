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
import com.island.ohara.client.configurator.v0.ZookeeperApi._
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.store.DataStore

import scala.concurrent.{ExecutionContext, Future}

object ZookeeperRoute {

  private[this] def updateState(info: ZookeeperClusterInfo)(
    implicit
    store: DataStore,
    clusterCollie: ClusterCollie,
    executionContext: ExecutionContext): Future[ZookeeperClusterInfo] = {
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
              store.addIfPresent[ZookeeperClusterInfo](info.name,
                                                       previous =>
                                                         Future.successful(
                                                           previous.copy(
                                                             state = finalData.state,
                                                             nodeNames = finalData.nodeNames,
                                                             deadNodes = finalData.deadNodes,
                                                           )
                                                       ))
          }
      )
  }

  def apply(implicit store: DataStore,
            clusterCollie: ClusterCollie,
            nodeCollie: NodeCollie,
            executionContext: ExecutionContext): server.Route =
    RouteUtils.basicRoute(
      root = ZOOKEEPER_PREFIX_PATH,
      hookOfCreation = (req: Creation) =>
        Future.successful(
          ZookeeperClusterInfo(
            name = req.name,
            imageName = req.imageName,
            clientPort = req.clientPort,
            peerPort = req.peerPort,
            electionPort = req.electionPort,
            nodeNames = req.nodeNames,
            deadNodes = Set.empty,
            tags = req.tags,
            state = None,
            error = None,
            lastModified = CommonUtils.current()
          )),
      hookOfUpdate = (name: String, req: Update, previousOption: Option[ZookeeperClusterInfo]) => {
        val updateReq = previousOption.fold(
          ZookeeperClusterInfo(
            name = name,
            imageName = req.imageName.getOrElse(IMAGE_NAME_DEFAULT),
            clientPort = req.clientPort.getOrElse(CommonUtils.availablePort()),
            peerPort = req.peerPort.getOrElse(CommonUtils.availablePort()),
            electionPort = req.electionPort.getOrElse(CommonUtils.availablePort()),
            nodeNames = req.nodeNames.getOrElse(Set.empty),
            deadNodes = Set.empty,
            tags = req.tags.getOrElse(Map.empty),
            state = None,
            error = None,
            lastModified = CommonUtils.current()
          )) {
          previous =>
            previous.copy(
              imageName = req.imageName.getOrElse(previous.imageName),
              clientPort = req.clientPort.getOrElse(previous.clientPort),
              peerPort = req.peerPort.getOrElse(previous.peerPort),
              electionPort = req.electionPort.getOrElse(previous.electionPort),
              nodeNames = req.nodeNames.getOrElse(previous.nodeNames),
              tags = req.tags.getOrElse(previous.tags),
              lastModified = CommonUtils.current()
            )
        }
        if (updateReq.state.isDefined)
          throw new RuntimeException(s"You cannot update property on non-stopped zookeeper cluster: $name")
        else Future.successful(updateReq)
      },
      hookBeforeDelete = (name: String) =>
        store.get[ZookeeperClusterInfo](name).flatMap {
          _.fold(Future.successful(name)) { info =>
            updateState(info).flatMap { data =>
              if (data.state.isEmpty) Future.successful(name)
              else Future.failed(new RuntimeException(s"You cannot delete a non-stopped zookeeper :$name"))
            }
          }
      },
      hookOfGet = (res: ZookeeperClusterInfo) => updateState(res),
      hookOfList = (res: Seq[ZookeeperClusterInfo]) => Future.traverse(res)(updateState)
    ) ~
      RouteUtils.appendRouteOfClusterAction(
        collie = clusterCollie.zookeeperCollie,
        root = ZOOKEEPER_PREFIX_PATH,
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
