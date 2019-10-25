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

import com.island.ohara.agent.{ServiceState, DataCollie, StreamCollie}
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.Definition
import com.island.ohara.client.configurator.v0.FileInfoApi.FileInfo
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.StreamApi.{StreamClusterInfo, StreamClusterStatus}
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.route.StreamRoute
import com.island.ohara.metrics.basic.{Counter, CounterMBean}
import com.island.ohara.streams.config.StreamDefUtils

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

private[configurator] class FakeStreamCollie(node: DataCollie)
    extends FakeCollie[StreamClusterStatus](node)
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
    (_, creation) =>
      if (clusterCache.asScala.exists(_._1.key == creation.key))
        Future.failed(new IllegalArgumentException(s"streamapp can't increase nodes at runtime"))
      else
        Future.successful(
          addCluster(
            new StreamClusterStatus(
              group = creation.group,
              name = creation.name,
              aliveNodes = creation.nodeNames,
              // In fake mode, we need to assign a state in creation for "GET" method to act like real case
              state = Some(ServiceState.RUNNING.name),
              error = None
            ),
            creation.imageName,
            creation.ports
          ))

  override protected def doRemoveNode(previousCluster: StreamClusterStatus, beRemovedContainer: ContainerInfo)(
    implicit executionContext: ExecutionContext): Future[Boolean] =
    Future.failed(
      new UnsupportedOperationException("stream collie doesn't support to remove node from a running cluster"))

  override protected def doCreator(executionContext: ExecutionContext,
                                   containerName: String,
                                   containerInfo: ContainerInfo,
                                   node: Node,
                                   route: Map[String, String],
                                   jarInfo: FileInfo): Future[Unit] = Future.unit

  override protected def dataCollie: DataCollie = node

  override protected def prefixKey: String = "fakestream"

  /**
    * in fake mode, we never return empty result or exception.
    *
    * @param jarUrl the custom streamApp jar url
    * @param executionContext thread pool
    * @return stream definition
    */
  override def loadDefinition(jarUrl: URL)(implicit executionContext: ExecutionContext): Future[Definition] =
    super.loadDefinition(jarUrl).recover {
      case _: Throwable =>
        Definition("fake_class", StreamDefUtils.DEFAULT.asScala.toList)
      // a serializable collection
    }

  override protected def brokerContainers(clusterKey: ObjectKey)(
    implicit executionContext: ExecutionContext): Future[Seq[ContainerInfo]] =
    throw new UnsupportedOperationException
}
