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

import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.atomic.AtomicInteger

import com.island.ohara.agent.docker.ContainerState
import com.island.ohara.agent.{ServiceState, Collie, NoSuchClusterException, NodeCollie}
import com.island.ohara.client.configurator.v0.ClusterStatus
import com.island.ohara.client.configurator.v0.ContainerApi.{ContainerInfo, PortMapping, PortPair}
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.common.util.CommonUtils

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
private[configurator] abstract class FakeCollie[T <: ClusterStatus: ClassTag](nodeCollie: NodeCollie)
    extends Collie[T] {
  protected val clusterCache =
    new ConcurrentSkipListMap[T, Seq[ContainerInfo]]((o1: T, o2: T) => o1.key.compareTo(o2.key))

  override protected def doRemoveNode(previousCluster: T, beRemovedContainer: ContainerInfo)(
    implicit executionContext: ExecutionContext): Future[Boolean] = {
    val containers = clusterCache.get(previousCluster)
    if (containers == null) Future.successful(false)
    else {
      clusterCache.put(previousCluster, containers.filter(_.name != beRemovedContainer.name))
      Future.successful(true)
    }
  }

  /**
    * update the in-memory cluster status and container infos
    * @param cluster cluster status
    * @param nodeNames node names
    * @return cluster status
    */
  private[configurator] def addCluster(cluster: T, imageName: String, nodeNames: Set[String], ports: Set[Int]): T = {
    val FAKE_KIND_NAME = "FAKE"
    def genContainers(cluster: T): Seq[ContainerInfo] = nodeNames.map { nodeName =>
      ContainerInfo(
        nodeName = nodeName,
        id = CommonUtils.randomString(10),
        imageName = imageName,
        created = "unknown",
        state = ContainerState.RUNNING.name,
        FAKE_KIND_NAME,
        name = CommonUtils.randomString(10),
        size = "unknown",
        portMappings = Seq(PortMapping("fake host", ports.map(p => PortPair(p, p)).toSeq)),
        environments = Map.empty,
        hostname = CommonUtils.randomString(10)
      )
    }.toSeq
    clusterCache.remove(cluster)
    clusterCache.put(cluster, genContainers(cluster))
    cluster
  }
  override def exist(objectKey: ObjectKey)(implicit executionContext: ExecutionContext): Future[Boolean] =
    Future.successful(clusterCache.keySet.asScala.exists(_.key == objectKey))

  override protected def doRemove(clusterInfo: T, containerInfos: Seq[ContainerInfo])(
    implicit executionContext: ExecutionContext): Future[Boolean] =
    Future.successful(clusterCache.remove(clusterInfo) != null)

  override def logs(objectKey: ObjectKey)(
    implicit executionContext: ExecutionContext): Future[Map[ContainerInfo, String]] =
    exist(objectKey).flatMap(if (_) Future.successful {
      val containers = clusterCache.asScala.find(_._1.key == objectKey).get._2
      containers.map(_ -> "fake log").toMap
    } else Future.failed(new NoSuchClusterException(s"$objectKey doesn't exist")))

  override def containers(objectKey: ObjectKey)(
    implicit executionContext: ExecutionContext): Future[Seq[ContainerInfo]] =
    exist(objectKey).map(if (_) clusterCache.asScala.find(_._1.key == objectKey).get._2 else Seq.empty)

  override def clusterWithAllContainers()(
    implicit executionContext: ExecutionContext): Future[Map[T, Seq[ContainerInfo]]] =
    Future.successful(clusterCache.asScala.toMap)

  private[this] val _forceRemoveCount = new AtomicInteger(0)
  override protected def doForceRemove(clusterInfo: T, containerInfos: Seq[ContainerInfo])(
    implicit executionContext: ExecutionContext): Future[Boolean] =
    try doRemove(clusterInfo, containerInfos)
    finally _forceRemoveCount.incrementAndGet()

  // In fake mode, the cluster state should be running since we add "running containers" always
  override protected def toClusterState(containers: Seq[ContainerInfo]): Option[ServiceState] =
    Some(ServiceState.RUNNING)

  def forceRemoveCount: Int = _forceRemoveCount.get()
}
