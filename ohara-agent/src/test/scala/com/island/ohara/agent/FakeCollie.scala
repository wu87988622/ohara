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

package com.island.ohara.agent

import com.island.ohara.agent.docker.ContainerState
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.MetricsApi.Metrics
import com.island.ohara.client.configurator.v0.{ClusterInfo, MetricsApi}
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.common.util.CommonUtils
import spray.json.JsValue

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

/**
  * This class only support Collie abstract class logic test.
  * No real to implement
  */
class FakeCollie[T <: FakeCollieClusterInfo: ClassTag](nodeCollie: NodeCollie, containers: Seq[ContainerInfo])
    extends Collie[FakeCollieClusterInfo] {

  override protected def doRemoveNode(previousCluster: FakeCollieClusterInfo, beRemovedContainer: ContainerInfo)(
    implicit executionContext: ExecutionContext): Future[Boolean] =
    Future.successful(true)

  override protected def doRemove(clusterInfo: FakeCollieClusterInfo, containerInfos: Seq[ContainerInfo])(
    implicit executionContext: ExecutionContext): Future[Boolean] = Future.successful(true)

  override def logs(objectKey: ObjectKey)(
    implicit executionContext: ExecutionContext): Future[Map[ContainerInfo, String]] =
    Future.successful(Map.empty)

  override def clusterWithAllContainers()(
    implicit executionContext: ExecutionContext): Future[Map[FakeCollieClusterInfo, Seq[ContainerInfo]]] =
    Future.successful(
      Map(
        FakeCollieClusterInfo(FakeCollie.key,
                              containers.map(c => c.nodeName).toSet,
                              toClusterState(containers).map(_.name)) -> containers))

  override def creator: FakeCollie.FakeClusterCreator =
    () => Future.successful(FakeCollieClusterInfo(FakeCollie.key, Set.empty, None))

  override protected def toClusterState(containers: Seq[ContainerInfo]): Option[ClusterState] =
    if (containers.isEmpty) None
    else {
      // one of the containers in pending state means cluster pending
      if (containers.exists(_.state == ContainerState.CREATED.name)) Some(ClusterState.PENDING)
      // not pending, if one of the containers in running state means cluster running (even other containers are in
      // restarting, paused, exited or dead state
      else if (containers.exists(_.state == ContainerState.RUNNING.name)) Some(ClusterState.RUNNING)
      // exists one container in dead state, and others are in exited state means cluster failed
      else if (containers.exists(_.state == ContainerState.DEAD.name) &&
               containers.forall(c => c.state == ContainerState.EXITED.name || c.state == ContainerState.DEAD.name))
        Some(ClusterState.FAILED)
      // we don't care other situation for now
      else Some(ClusterState.UNKNOWN)
    }

  override def serviceName: String = "fake"
}

case class FakeCollieClusterInfo(objectKey: ObjectKey, nodeNames: Set[String], state: Option[String])
    extends ClusterInfo {
  override def name: String = objectKey.name()

  override def group: String = objectKey.group()

  override def imageName: String = "I DON'T CARE"

  override def deadNodes: Set[String] = Set.empty

  override def ports: Set[Int] = Set.empty

  override def lastModified: Long = CommonUtils.current()

  override def kind: String = "fake_cluster"

  override def tags: Map[String, JsValue] = Map.empty

  override def error: Option[String] = None

  override def metrics: MetricsApi.Metrics = Metrics.EMPTY

  override def settings: Map[String, JsValue] = throw new UnsupportedOperationException
}

object FakeCollie {
  private[this] val clusterName: String = "fakecluster1"
  private[this] val group: String = "fakegroup"
  protected[agent] val key: ObjectKey = ObjectKey.of(group, clusterName)
  val FAKE_SERVICE_NAME: String = "fake"
  trait FakeClusterCreator extends Collie.ClusterCreator[FakeCollieClusterInfo] {
    override def group(group: String): FakeClusterCreator.this.type =
      throw new UnsupportedOperationException
    override def imageName(imageName: String): FakeClusterCreator.this.type =
      throw new UnsupportedOperationException
    override def nodeNames(nodeNames: Set[String]): FakeClusterCreator.this.type =
      throw new UnsupportedOperationException
  }
}
