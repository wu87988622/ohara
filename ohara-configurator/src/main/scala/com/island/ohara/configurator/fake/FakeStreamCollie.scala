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
import com.island.ohara.client.configurator.v0.MetricsApi.Metrics
import com.island.ohara.client.configurator.v0.{NodeApi, StreamApi}
import com.island.ohara.client.configurator.v0.StreamApi.StreamClusterInfo
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.kafka.connector.json.ObjectKey
import com.island.ohara.metrics.BeanChannel
import com.island.ohara.metrics.basic.CounterMBean

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

private[configurator] class FakeStreamCollie(node: NodeCollie)
    extends FakeCollie[StreamClusterInfo, StreamCollie.ClusterCreator](node)
    with StreamCollie {

  override def counters(cluster: StreamClusterInfo): Seq[CounterMBean] =
    // we don't care for the fake mode since both fake mode and embedded mode are running on local jvm
    BeanChannel.local().counterMBeans().asScala

  override def creator: StreamCollie.ClusterCreator =
    (clusterName, nodeNames, imageName, _, _, _, from, to, jmxPort, _, _) =>
      Future.successful(
        addCluster(
          StreamApi.StreamClusterInfo(
            name = clusterName,
            imageName = imageName,
            instances = nodeNames.size,
            jar = ObjectKey.of("fakeGroup", "fakeJar"),
            from = from,
            to = to,
            metrics = Metrics(Seq.empty),
            nodeNames = nodeNames,
            deadNodes = Set.empty,
            jmxPort = jmxPort,
            state = Some(ContainerState.RUNNING.name),
            error = None,
            lastModified = CommonUtils.current(),
            tags = Map.empty
          )))

  override protected def doRemoveNode(previousCluster: StreamClusterInfo, beRemovedContainer: ContainerInfo)(
    implicit executionContext: ExecutionContext): Future[Boolean] =
    Future.failed(
      new UnsupportedOperationException("stream collie doesn't support to remove node from a running cluster"))

  override protected def doAddNode(
    previousCluster: StreamClusterInfo,
    previousContainers: Seq[ContainerInfo],
    newNodeName: String)(implicit executionContext: ExecutionContext): Future[StreamClusterInfo] =
    Future.failed(new UnsupportedOperationException("stream collie doesn't support to add node from a running cluster"))

  override protected def doCreator(executionContext: ExecutionContext,
                                   clusterName: String,
                                   containerName: String,
                                   containerInfo: ContainerInfo,
                                   node: NodeApi.Node,
                                   jmxPort: Int,
                                   route: Map[String, String]): Future[Unit] =
    throw new UnsupportedOperationException("stream collie doesn't support to doCreator function")

  override protected def nodeCollie: NodeCollie = node

  override protected def prefixKey: String = "fakestream"
}
