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

import java.util.concurrent.ConcurrentSkipListMap

import com.island.ohara.agent.{BrokerCollie, DataCollie, NoSuchClusterException}
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.{ClusterStatus, NodeApi}
import com.island.ohara.client.kafka.TopicAdmin
import com.island.ohara.common.annotations.VisibleForTesting
import com.island.ohara.metrics.BeanChannel
import com.island.ohara.metrics.kafka.TopicMeter

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

private[configurator] class FakeBrokerCollie(node: DataCollie, bkConnectionProps: String)
    extends FakeCollie(node)
    with BrokerCollie {
  override def topicMeters(cluster: BrokerClusterInfo): Seq[TopicMeter] =
    // we don't care for the fake mode since both fake mode and embedded mode are run on local jvm
    BeanChannel.local().topicMeters().asScala

  /**
    * cache all topics info in-memory so we should keep instance for each fake cluster.
    */
  @VisibleForTesting
  private[configurator] val fakeAdminCache = new ConcurrentSkipListMap[BrokerClusterInfo, FakeTopicAdmin](
    (o1: BrokerClusterInfo, o2: BrokerClusterInfo) => o1.key.compareTo(o2.key)
  )

  override def creator: BrokerCollie.ClusterCreator =
    (_, creation) =>
      Future.successful(
        addCluster(
          key = creation.key,
          kind = ClusterStatus.Kind.BROKER,
          nodeNames = creation.nodeNames ++ clusterCache.asScala
            .find(_._1 == creation.key)
            .map(_._2.nodeNames)
            .getOrElse(Set.empty),
          imageName = creation.imageName,
          ports = creation.ports
        )
      )

  override def topicAdmin(
    brokerClusterInfo: BrokerClusterInfo
  )(implicit executionContext: ExecutionContext): Future[TopicAdmin] =
    if (bkConnectionProps != null) Future.successful(TopicAdmin(bkConnectionProps))
    else if (clusterCache.keySet().asScala.contains(brokerClusterInfo.key)) {
      val fake = new FakeTopicAdmin
      val r    = fakeAdminCache.putIfAbsent(brokerClusterInfo, fake)
      Future.successful(if (r == null) fake else r)
    } else
      Future.failed(new NoSuchClusterException(s"cluster:${brokerClusterInfo.key} is not running"))

  override protected def doCreator(
    executionContext: ExecutionContext,
    containerInfo: ContainerInfo,
    node: NodeApi.Node,
    route: Map[String, String],
    arguments: Seq[String]
  ): Future[Unit] =
    throw new UnsupportedOperationException("Fake broker collie doesn't support doCreator function")

  override protected def dataCollie: DataCollie = node

  /**
    * Implement prefix name for the platform
    *
    * @return
    */
  override protected def prefixKey: String = "fakebroker"
}
