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

import java.net.URL

import com.island.ohara.agent.{ClusterState, NodeCollie, StreamCollie}
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.FileInfoApi.FileInfo
import com.island.ohara.client.configurator.v0.MetricsApi.Metrics
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.StreamApi.StreamClusterInfo
import com.island.ohara.client.configurator.v0.{Definition, StreamApi}
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.route.StreamRoute
import com.island.ohara.metrics.basic.{Counter, CounterMBean}
import com.island.ohara.streams.config.StreamDefUtils

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

private[configurator] class FakeStreamCollie(node: NodeCollie)
    extends FakeCollie[StreamClusterInfo](node)
    with StreamCollie {

  override def counters(cluster: StreamClusterInfo): Seq[CounterMBean] =
    // we fake counters since streamApp is not really running in fake collie mode
    Seq(
      Counter
        .builder()
        .group(StreamRoute.STREAM_APP_GROUP)
        .name("fakeCounter")
        .value(CommonUtils.randomInteger().toLong)
        .build())

  override def creator: StreamCollie.ClusterCreator =
    (_, nodeNames, _, _, _, _, _, _, settings, _) =>
      Future.successful(
        addCluster(
          StreamApi.StreamClusterInfo(
            settings = settings,
            // convert to list in order to be serializable
            definition = Some(Definition("fake_class", StreamDefUtils.DEFAULT.asScala.toList)),
            nodeNames = nodeNames,
            deadNodes = Set.empty,
            // In fake mode, we need to assign a state in creation for "GET" method to act like real case
            state = Some(ClusterState.RUNNING.name),
            error = None,
            metrics = Metrics(Seq.empty),
            lastModified = CommonUtils.current()
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
                                   containerName: String,
                                   containerInfo: ContainerInfo,
                                   node: Node,
                                   route: Map[String, String],
                                   jmxPort: Int,
                                   jarInfo: FileInfo): Future[Unit] = Future.unit

  override protected def nodeCollie: NodeCollie = node

  override protected def prefixKey: String = "fakestream"

  /**
    * in fake mode, we never return empty result or exception.
    *
    * @param jarUrl the custom streamApp jar url
    * @param executionContext thread pool
    * @return stream definition
    */
  override def loadDefinition(jarUrl: URL)(implicit executionContext: ExecutionContext): Future[Option[Definition]] =
    super
      .loadDefinition(jarUrl)
      .recover {
        case _: Throwable =>
          Some(Definition("fake_class", StreamDefUtils.DEFAULT.asScala.toList)) // a serializable collection
      }
      .map(_.orElse(Some(Definition("fake_class", StreamDefUtils.DEFAULT.asScala.toList)))) // a serializable collection

  override protected def brokerContainers(clusterName: String)(
    implicit executionContext: ExecutionContext): Future[Seq[ContainerInfo]] =
    throw new UnsupportedOperationException
}
