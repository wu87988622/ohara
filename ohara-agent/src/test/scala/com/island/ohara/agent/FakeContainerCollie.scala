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

import com.island.ohara.agent.Collie.ClusterCreator
import com.island.ohara.client.configurator.v0.ClusterInfo
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.{ClassTag, classTag}

/**
  * This class only support ContainerCollie abstract class logic test.
  * No real to implement
  * @param nodeCollie
  * @param containers
  * @param classTag$T
  * @tparam T
  * @tparam Creator
  */
class FakeContainerCollie[T <: FakeContainerCollieClusterInfo: ClassTag, Creator <: ClusterCreator[T]](
  nodeCollie: NodeCollie,
  containers: Seq[ContainerInfo])
    extends ContainerCollie[FakeContainerCollieClusterInfo, FakeCollie.ClusterCreator](nodeCollie) {

  override protected def doRemoveNode(
    previousCluster: FakeContainerCollieClusterInfo,
    beRemovedContainer: ContainerInfo)(implicit executionContext: ExecutionContext): Future[Boolean] =
    Future.successful(true)

  override protected def doAddNode(
    previousCluster: FakeContainerCollieClusterInfo,
    previousContainers: Seq[ContainerInfo],
    newNodeName: String)(implicit executionContext: ExecutionContext): Future[FakeContainerCollieClusterInfo] =
    Future {
      FakeContainerCollieClusterInfo(previousCluster.name, previousCluster.nodeNames ++ Seq(newNodeName))
    }

  override protected def doRemove(clusterInfo: FakeContainerCollieClusterInfo, containerInfos: Seq[ContainerInfo])(
    implicit executionContext: ExecutionContext): Future[Boolean] = Future.successful(true)

  override def logs(clusterName: String)(
    implicit executionContext: ExecutionContext): Future[Map[ContainerInfo, String]] = {
    Future {
      Map()
    }
  }

  override def clusters(
    implicit executionContext: ExecutionContext): Future[Map[FakeContainerCollieClusterInfo, Seq[ContainerInfo]]] =
    Future {
      val nodeNames = containers.map(c => c.nodeName)
      Map(FakeContainerCollieClusterInfo(FakeContainerCollie.clusterName, nodeNames) -> containers)
    }

  override def creator(): FakeCollie.ClusterCreator = {
    new FakeCollie.ClusterCreator {
      override def create()(implicit executionContext: ExecutionContext): Future[FakeContainerCollieClusterInfo] =
        Future {
          FakeContainerCollieClusterInfo(FakeContainerCollie.clusterName, Seq.empty)
        }
    }
  }

  override protected def serviceName: String =
    if (classTag[T].runtimeClass.isAssignableFrom(classOf[FakeContainerCollieClusterInfo])) FakeCollie.FAKE_SERVICE_NAME
    else throw new IllegalArgumentException(s"Who are you, ${classTag[T].runtimeClass} ???")
}

case class FakeContainerCollieClusterInfo(name: String, nodeNames: Seq[String]) extends ClusterInfo {
  override def imageName: String = "I DON'T CARE"

  override def ports: Set[Int] = Set.empty

  override def clone(newNodeNames: Seq[String]): ClusterInfo = throw new UnsupportedOperationException
}

object FakeContainerCollie {
  protected[agent] val clusterName: String = "fakecluster1"
}

object FakeCollie {
  val FAKE_SERVICE_NAME: String = "fake"
  trait ClusterCreator extends Collie.ClusterCreator[FakeContainerCollieClusterInfo] {}
}
