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

import java.util.concurrent.atomic.AtomicInteger

import com.island.ohara.agent.Collie.ClusterCreator
import com.island.ohara.agent.docker.ContainerState
import com.island.ohara.agent.{ContainerCollie, NoSuchClusterException, NodeCollie}
import com.island.ohara.client.configurator.v0.ClusterInfo
import com.island.ohara.client.configurator.v0.ContainerApi.{ContainerInfo, PortMapping, PortPair}
import com.island.ohara.common.util.CommonUtils

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
private[configurator] abstract class FakeCollie[T <: ClusterInfo: ClassTag, Creator <: ClusterCreator[T]](
  nodeCollie: NodeCollie)
    extends ContainerCollie[T, Creator](nodeCollie) {
  protected val clusterCache = new mutable.HashMap[T, Seq[ContainerInfo]]()

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
        environments = Map("WORKER_PLUGINS" -> "http://worker"),
        hostname = CommonUtils.randomString(10)
      )
    }
    clusterCache.put(cluster, genContainers(cluster))
    cluster
  }
  override def exist(clusterName: String)(implicit executionContext: ExecutionContext): Future[Boolean] =
    Future.successful(clusterCache.keys.exists(_.name == clusterName))

  override def remove(clusterName: String)(implicit executionContext: ExecutionContext): Future[T] =
    exist(clusterName).flatMap(if (_) Future.successful {
      val cluster = clusterCache.keys.find(_.name == clusterName).get
      clusterCache.remove(cluster)
      cluster
    } else Future.failed(new NoSuchClusterException(s"$clusterName doesn't exist")))

  override def logs(clusterName: String)(
    implicit executionContext: ExecutionContext): Future[Map[ContainerInfo, String]] =
    exist(clusterName).flatMap(if (_) Future.successful {
      val containers = clusterCache.find(_._1.name == clusterName).get._2
      containers.map(_ -> "fake log").toMap
    } else Future.failed(new NoSuchClusterException(s"$clusterName doesn't exist")))

  override def containers(clusterName: String)(
    implicit executionContext: ExecutionContext): Future[Seq[ContainerInfo]] =
    exist(clusterName).map(if (_) clusterCache.find(_._1.name == clusterName).get._2 else Seq.empty)

  override def clusters(implicit executionContext: ExecutionContext): Future[Map[T, Seq[ContainerInfo]]] =
    Future.successful(clusterCache.toMap)

  private[this] val _forceRemoveCount = new AtomicInteger(0)
  override def forceRemove(clusterName: String)(implicit executionContext: ExecutionContext): Future[T] = {
    _forceRemoveCount.incrementAndGet()
    remove(clusterName)
  }
  def forceRemoveCount: Int = _forceRemoveCount.get()
}
