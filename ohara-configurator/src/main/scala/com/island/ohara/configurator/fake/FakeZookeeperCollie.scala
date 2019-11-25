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

import com.island.ohara.agent.{DataCollie, ZookeeperCollie}
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.{ClusterStatus, NodeApi}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

private[configurator] class FakeZookeeperCollie(dataCollie: DataCollie)
    extends FakeCollie(dataCollie)
    with ZookeeperCollie {
  override def creator: ZookeeperCollie.ClusterCreator =
    (_, creation) =>
      if (clusterCache.asScala.exists(_._1 == creation.key))
        Future.failed(new IllegalArgumentException(s"zookeeper can't increase nodes at runtime"))
      else
        Future.successful(
          addCluster(
            key = creation.key,
            kind = ClusterStatus.Kind.ZOOKEEPER,
            nodeNames = creation.nodeNames ++ clusterCache.asScala
              .find(_._1 == creation.key)
              .map(_._2.nodeNames)
              .getOrElse(Set.empty),
            imageName = creation.imageName,
            ports = creation.ports
          )
        )

  override protected def doCreator(
    executionContext: ExecutionContext,
    containerInfo: ContainerInfo,
    node: NodeApi.Node,
    route: Map[String, String],
    arguments: Seq[String]
  ): Future[Unit] =
    throw new UnsupportedOperationException("zookeeper collie doesn't support to doCreator function")
}
