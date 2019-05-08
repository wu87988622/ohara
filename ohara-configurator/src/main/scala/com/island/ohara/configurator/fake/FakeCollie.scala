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
import java.util.concurrent.atomic.AtomicInteger

import com.island.ohara.agent.Collie.ClusterCreator
import com.island.ohara.agent.docker.ContainerState
import com.island.ohara.agent.{ContainerCollie, NoSuchClusterException, NodeCollie, WorkerCollie}
import com.island.ohara.client.configurator.v0.ClusterInfo
import com.island.ohara.client.configurator.v0.ContainerApi.{ContainerInfo, PortMapping, PortPair}
import com.island.ohara.common.util.CommonUtils

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.collection.JavaConverters._
private[configurator] abstract class FakeCollie[T <: ClusterInfo: ClassTag, Creator <: ClusterCreator[T]](
  nodeCollie: NodeCollie)
    extends ContainerCollie[T, Creator](nodeCollie) {
  protected val clusterCache = new ConcurrentHashMap[T, Seq[ContainerInfo]]()

  def addCluster(cluster: T): T = {
    val FAKE_KIND_NAME = "FAKE"
    def genContainers(cluster: T): Seq[ContainerInfo] = cluster.nodeNames.map { nodeName =>
      ContainerInfo(
        nodeName = nodeName,
        id = CommonUtils.randomString(10),
        imageName = cluster.imageName,
        created = "unknown",
        state = ContainerState.RUNNING.name,
        FAKE_KIND_NAME,
        name = CommonUtils.randomString(10),
        size = "unknown",
        portMappings = Seq(PortMapping("fake host", cluster.ports.map(p => PortPair(p, p)).toSeq)),
        environments = Map(WorkerCollie.JARS_KEY -> "http://worker"),
        hostname = CommonUtils.randomString(10)
      )
    }
    clusterCache.put(cluster, genContainers(cluster))
    cluster
  }
  override def exist(clusterName: String)(implicit executionContext: ExecutionContext): Future[Boolean] =
    Future.successful(clusterCache.keys.asScala.exists(_.name == clusterName))

  override protected def doRemove(clusterInfo: T, containerInfos: Seq[ContainerInfo])(
    implicit executionContext: ExecutionContext): Future[Boolean] =
    Future.successful(clusterCache.remove(clusterInfo) != null)

  override def logs(clusterName: String)(
    implicit executionContext: ExecutionContext): Future[Map[ContainerInfo, String]] =
    exist(clusterName).flatMap(if (_) Future.successful {
      val containers = clusterCache.asScala.find(_._1.name == clusterName).get._2
      containers.map(_ -> "fake log").toMap
    } else Future.failed(new NoSuchClusterException(s"$clusterName doesn't exist")))

  override def containers(clusterName: String)(
    implicit executionContext: ExecutionContext): Future[Seq[ContainerInfo]] =
    exist(clusterName).map(if (_) clusterCache.asScala.find(_._1.name == clusterName).get._2 else Seq.empty)

  override def clusters(implicit executionContext: ExecutionContext): Future[Map[T, Seq[ContainerInfo]]] =
    Future.successful(clusterCache.asScala.toMap)

  private[this] val _forceRemoveCount = new AtomicInteger(0)
  override protected def doForceRemove(clusterInfo: T, containerInfos: Seq[ContainerInfo])(
    implicit executionContext: ExecutionContext): Future[Boolean] =
    try doRemove(clusterInfo, containerInfos)
    finally _forceRemoveCount.incrementAndGet()

  def forceRemoveCount: Int = _forceRemoveCount.get()
}
