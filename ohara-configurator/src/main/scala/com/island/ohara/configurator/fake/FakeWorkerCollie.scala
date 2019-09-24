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

package com.island.ohara.configurator.fake

import java.util.concurrent.ConcurrentHashMap

import com.island.ohara.agent.{ClusterState, NoSuchClusterException, NodeCollie, WorkerCollie}
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.{NodeApi, WorkerApi}
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.client.kafka.WorkerClient
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.metrics.BeanChannel
import com.island.ohara.metrics.basic.CounterMBean

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

private[configurator] class FakeWorkerCollie(node: NodeCollie, wkConnectionProps: String)
    extends FakeCollie[WorkerClusterInfo](node)
    with WorkerCollie {

  override def counters(cluster: WorkerClusterInfo): Seq[CounterMBean] =
    // we don't care for the fake mode since both fake mode and embedded mode are run on local jvm
    BeanChannel.local().counterMBeans().asScala

  /**
    * cache all connectors info in-memory so we should keep instance for each fake cluster.
    */
  private[this] val fakeClientCache = new ConcurrentHashMap[WorkerClusterInfo, FakeWorkerClient]
  override def creator: WorkerCollie.ClusterCreator = (_, creation) =>
    Future.successful(
      addCluster(
        WorkerClusterInfo(
          settings = WorkerApi.access.request
            .settings(creation.settings)
            .nodeNames(creation.nodeNames ++ clusterCache.asScala
              .find(_._1.key == creation.key)
              .map(_._1.nodeNames)
              .getOrElse(Set.empty))
            .creation
            .settings,
          connectors = FakeWorkerClient.localConnectorDefinitions,
          aliveNodes = creation.nodeNames ++ clusterCache.asScala
            .find(_._1.key == creation.key)
            .map(_._1.nodeNames)
            .getOrElse(Set.empty),
          // In fake mode, we need to assign a state in creation for "GET" method to act like real case
          state = Some(ClusterState.RUNNING.name),
          error = None,
          lastModified = CommonUtils.current()
        )))

  override protected def doRemoveNode(previousCluster: WorkerClusterInfo, beRemovedContainer: ContainerInfo)(
    implicit executionContext: ExecutionContext): Future[Boolean] = Future
    .successful(
      addCluster(
        previousCluster
          .newNodeNames(previousCluster.nodeNames.filterNot(_ == beRemovedContainer.nodeName))
          .asInstanceOf[WorkerClusterInfo]))
    .map(_ => true)

  override def workerClient(cluster: WorkerClusterInfo): WorkerClient =
    if (wkConnectionProps == null) {
      if (!clusterCache.containsKey(cluster))
        throw new NoSuchClusterException(s"worker cluster:$cluster does not exist")
      val fake = FakeWorkerClient()
      val r = fakeClientCache.putIfAbsent(cluster, fake)
      if (r == null) fake else r
    } else WorkerClient.builder.connectionProps(wkConnectionProps).build

  override protected def doCreator(executionContext: ExecutionContext,
                                   containerName: String,
                                   containerInfo: ContainerInfo,
                                   node: NodeApi.Node,
                                   route: Map[String, String]): Future[Unit] =
    throw new UnsupportedOperationException("FakeWorkerCollie doesn't support doCreator function")

  override protected def brokerContainers(clusterName: String)(
    implicit executionContext: ExecutionContext): Future[Seq[ContainerInfo]] =
    throw new UnsupportedOperationException("FakeWorkerCollie doesn't support brokerClusters function")

  override protected def nodeCollie: NodeCollie = node

  override protected def prefixKey: String = "fakeworker"
}
