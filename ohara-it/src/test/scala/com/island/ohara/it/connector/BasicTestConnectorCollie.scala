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

package com.island.ohara.it.connector

import java.net.URL

import com.island.ohara.agent.ClusterCollie
import com.island.ohara.client.configurator.v0.FileInfoApi.FileInfo
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.{BrokerApi, ContainerApi, WorkerApi, ZookeeperApi}
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.it.IntegrationTest
import com.island.ohara.it.agent.ClusterNameHolder
import org.scalatest.Matchers

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

abstract class BasicTestConnectorCollie extends IntegrationTest with Matchers {

  protected val nodeCache: Seq[Node]

  protected def clusterCollie(): ClusterCollie

  protected def jdbcJarUrl(): String

  /** to simplify test, we use the same group for ALL collie test
    * It is ok to use same group since we will use different cluster name
    */
  private[this] final val group: String = CommonUtils.randomString(10)

  protected[this] def cleanZookeeper(clusterName: String): Future[Unit] = {
    result(zk_stop(clusterName))
    await(() => {
      // In configurator mode: clusters() will return the "stopped list" in normal case
      // In collie mode: clusters() will return the "cluster list except stop one" in normal case
      // we should consider these two cases by case...
      val clusters = result(zk_clusters())
      !clusters.map(_.name).contains(clusterName) || clusters.find(_.name == clusterName).get.state.isEmpty
    })
    // the cluster is stopped actually, delete the data
    zk_delete(clusterName)
  }

  protected[this] def zk_clusters(): Future[Seq[ZookeeperApi.ZookeeperClusterInfo]] =
    clusterCollie.zookeeperCollie.clusters().map(_.keys.toSeq)

  protected[this] def zk_stop(clusterName: String): Future[Unit] =
    clusterCollie.zookeeperCollie.forceRemove(ObjectKey.of(group, clusterName)).map(_ => Unit)

  protected[this] def zk_containers(clusterName: String): Future[Seq[ContainerApi.ContainerInfo]] =
    clusterCollie.zookeeperCollie.containers(ObjectKey.of(group, clusterName))

  protected[this] def zk_start(clusterName: String): Future[Unit] =
    // We don't need to start a cluster in collie since we already start a cluster by create method
    Future.successful(Unit)

  protected[this] def zk_delete(clusterName: String): Future[Unit] =
    // We don't need to remove data stored in configurator in collie since there is nothing to do
    Future.successful(Unit)

  protected[this] def zk_create(clusterName: String,
                                clientPort: Int,
                                electionPort: Int,
                                peerPort: Int,
                                nodeNames: Set[String]): Future[ZookeeperApi.ZookeeperClusterInfo] =
    clusterCollie.zookeeperCollie.creator
      .group(group)
      .clusterName(clusterName)
      .imageName(ZookeeperApi.IMAGE_NAME_DEFAULT)
      .clientPort(clientPort)
      .peerPort(peerPort)
      .electionPort(electionPort)
      .nodeNames(nodeNames)
      .create()

  protected[this] def cleanBroker(clusterName: String): Future[Unit] = {
    result(bk_stop(clusterName))
    await(() => {
      // In configurator mode: clusters() will return the "stopped list" in normal case
      // In collie mode: clusters() will return the "cluster list except stop one" in normal case
      // we should consider these two cases by case...
      val clusters = result(bk_clusters())
      !clusters.map(_.name).contains(clusterName) || clusters.find(_.name == clusterName).get.state.isEmpty
    })
    // the cluster is stopped actually, delete the data
    bk_delete(clusterName)
  }

  protected[this] def bk_start(clusterName: String): Future[Unit] =
    // We don't need to start a cluster in collie since we already start a cluster by create method
    Future.successful(Unit)

  protected[this] def bk_clusters(): Future[Seq[BrokerApi.BrokerClusterInfo]] =
    clusterCollie.brokerCollie.clusters().map(_.keys.toSeq)

  protected[this] def bk_containers(clusterName: String): Future[Seq[ContainerApi.ContainerInfo]] =
    clusterCollie.brokerCollie.containers(ObjectKey.of(group, clusterName))

  protected[this] def bk_stop(clusterName: String): Future[Unit] =
    clusterCollie.brokerCollie.forceRemove(ObjectKey.of(group, clusterName)).map(_ => Unit)

  protected[this] def bk_delete(clusterName: String): Future[Unit] =
    // We don't need to remove data stored in configurator in collie since there is nothing to do
    Future.successful(Unit)

  protected[this] def bk_create(clusterName: String,
                                clientPort: Int,
                                exporterPort: Int,
                                jmxPort: Int,
                                zkClusterName: String,
                                nodeNames: Set[String]): Future[BrokerApi.BrokerClusterInfo] =
    clusterCollie.brokerCollie.creator
      .imageName(BrokerApi.IMAGE_NAME_DEFAULT)
      .group(group)
      .clusterName(clusterName)
      .clientPort(clientPort)
      .exporterPort(exporterPort)
      .jmxPort(jmxPort)
      .zookeeperClusterName(zkClusterName)
      .nodeNames(nodeNames)
      .create()

  protected[this] def wk_containers(clusterName: String): Future[Seq[ContainerApi.ContainerInfo]] =
    clusterCollie.workerCollie.containers(ObjectKey.of(group, clusterName))

  protected[this] def wk_exist(clusterName: String): Future[Boolean] =
    clusterCollie.workerCollie.exist(ObjectKey.of(group, clusterName))

  protected[this] def wk_clusters(): Future[Seq[WorkerApi.WorkerClusterInfo]] =
    clusterCollie.workerCollie.clusters().map(_.keys.toSeq)

  protected def wk_stop(clusterName: String): Future[Unit] =
    clusterCollie.workerCollie.forceRemove(ObjectKey.of(group, clusterName)).map(_ => Unit)

  protected[this] def wk_logs(clusterName: String): Future[Seq[String]] =
    clusterCollie.workerCollie.logs(ObjectKey.of(group, clusterName)).map(_.values.toSeq)

  protected[this] def wk_delete(clusterName: String): Future[Unit] =
    // We don't need to remove data stored in configurator in collie since there is nothing to do
    Future.successful(Unit)

  protected[this] def wk_create(clusterName: String,
                                clientPort: Int,
                                jmxPort: Int,
                                bkClusterName: String,
                                nodeNames: Set[String]): Future[WorkerApi.WorkerClusterInfo] =
    clusterCollie.workerCollie.creator
      .imageName(WorkerApi.IMAGE_NAME_DEFAULT)
      .clusterName(clusterName)
      .group(group)
      .clientPort(clientPort)
      .jmxPort(jmxPort)
      .brokerClusterName(bkClusterName)
      .groupId(CommonUtils.randomString(10))
      .configTopicName(CommonUtils.randomString(10))
      .configTopicReplications(1)
      .statusTopicName(CommonUtils.randomString(10))
      .statusTopicReplications(1)
      .statusTopicPartitions(1)
      .offsetTopicName(CommonUtils.randomString(10))
      .offsetTopicReplications(1)
      .offsetTopicPartitions(1)
      .jarInfos(
        Seq(
          FileInfo(
            name = CommonUtils.randomString(),
            group = CommonUtils.randomString(),
            size = 100,
            url = new URL(jdbcJarUrl()),
            lastModified = CommonUtils.current(),
            tags = Map.empty
          )))
      .nodeNames(nodeNames)
      .create()

  protected[this] def cleanWorker(clusterName: String): Future[Unit] = {
    result(wk_stop(clusterName))
    await(() => {
      // In configurator mode: clusters() will return the "stopped list" in normal case
      // In collie mode: clusters() will return the "cluster list except stop one" in normal case
      // we should consider these two cases by case...
      val clusters = result(wk_clusters())
      !clusters.map(_.name).contains(clusterName) || clusters.find(_.name == clusterName).get.state.isEmpty
    })
    // the cluster is stopped actually, delete the data
    wk_delete(clusterName)
  }

  protected def nameHolder: ClusterNameHolder

}
