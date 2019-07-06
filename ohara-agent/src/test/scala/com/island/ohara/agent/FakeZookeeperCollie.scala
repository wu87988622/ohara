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
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo
import scala.concurrent.{ExecutionContext, Future}

class FakeZookeeperCollie(node: NodeCollie) extends ZookeeperCollie {

  override protected def doCreator(executionContext: ExecutionContext,
                                   clusterName: String,
                                   containerName: String,
                                   containerInfo: ContainerInfo,
                                   node: Node,
                                   route: Map[String, String]): Unit = {
    //Nothing
  }

  override def remove(clusterName: String)(implicit executionContext: ExecutionContext): Future[Boolean] =
    throw new UnsupportedOperationException("Not support remove function")

  override def logs(clusterName: String)(
    implicit executionContext: ExecutionContext): Future[Map[ContainerInfo, String]] =
    throw new UnsupportedOperationException("Not support logs function")

  override def clusterWithAllContainers()(
    implicit executionContext: ExecutionContext): Future[Map[ZookeeperClusterInfo, Seq[ContainerInfo]]] = {
    Future {
      Map.empty
    }
  }

  override def addNode(clusterName: String, nodeName: String)(
    implicit executionContext: ExecutionContext): Future[ZookeeperClusterInfo] =
    throw new UnsupportedOperationException("Not support addNode function")

  override def removeNode(clusterName: String, nodeName: String)(
    implicit executionContext: ExecutionContext): Future[Boolean] =
    throw new UnsupportedOperationException("Not support removeNode function")

  override protected def routeInfo(nodes: Map[Node, String]): Map[String, String] = {
    Map.empty
  }

  override protected def nodeCollie: NodeCollie = node

  override protected def prefixKey: String = "fakezookeeper"

  override protected def serviceName: String = "zk"
}
