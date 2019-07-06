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

import com.island.ohara.agent.docker.ContainerState
import com.island.ohara.agent.{NodeCollie, StreamCollie}
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.StreamApi
import com.island.ohara.client.configurator.v0.StreamApi.StreamClusterInfo
import com.island.ohara.metrics.BeanChannel
import com.island.ohara.metrics.basic.CounterMBean

import scala.concurrent.{ExecutionContext, Future}
import scala.collection.JavaConverters._

private[configurator] class FakeStreamCollie(nodeCollie: NodeCollie)
    extends FakeCollie[StreamClusterInfo, StreamCollie.ClusterCreator](nodeCollie)
    with StreamCollie {

  override def counters(cluster: StreamClusterInfo): Seq[CounterMBean] =
    // we don't care for the fake mode since both fake mode and embedded mode are running on local jvm
    BeanChannel.local().counterMBeans().asScala

  override def creator: StreamCollie.ClusterCreator =
    (clusterName, nodeNames, imageName, _, _, _, _, _, jmxPort, _, executionContext) => {
      implicit val exec: ExecutionContext = executionContext
      nodeCollie.nodes(nodeNames).map { nodes =>
        addCluster(
          StreamApi.StreamClusterInfo(
            name = clusterName,
            imageName = imageName,
            nodeNames = nodes.map(_.name).toSet,
            jmxPort = jmxPort,
            deadNodes = Set.empty,
            state = Some(ContainerState.RUNNING.name)
          )
        )
      }
    }

  override protected def doRemoveNode(previousCluster: StreamClusterInfo, beRemovedContainer: ContainerInfo)(
    implicit executionContext: ExecutionContext): Future[Boolean] =
    Future.failed(
      new UnsupportedOperationException("stream collie doesn't support to remove node from a running cluster"))

  override protected def doAddNode(
    previousCluster: StreamClusterInfo,
    previousContainers: Seq[ContainerInfo],
    newNodeName: String)(implicit executionContext: ExecutionContext): Future[StreamClusterInfo] =
    Future.failed(new UnsupportedOperationException("stream collie doesn't support to add node from a running cluster"))
}
