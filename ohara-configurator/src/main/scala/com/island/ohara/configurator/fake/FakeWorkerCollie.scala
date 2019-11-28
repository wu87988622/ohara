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

import com.island.ohara.agent.{DataCollie, NoSuchClusterException, WorkerCollie}
import com.island.ohara.client.configurator.v0.ClusterStatus
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.client.kafka.WorkerClient
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.metrics.BeanChannel
import com.island.ohara.metrics.basic.CounterMBean

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

private[configurator] class FakeWorkerCollie(dataCollie: DataCollie, wkConnectionProps: String)
    extends FakeCollie(dataCollie)
    with WorkerCollie {
  override def counters(cluster: WorkerClusterInfo): Seq[CounterMBean] =
    // we don't care for the fake mode since both fake mode and embedded mode are run on local jvm
    BeanChannel.local().counterMBeans().asScala

  /**
    * cache all connectors info in-memory so we should keep instance for each fake cluster.
    */
  private[this] val fakeClientCache = new ConcurrentSkipListMap[WorkerClusterInfo, FakeWorkerClient](
    (o1: WorkerClusterInfo, o2: WorkerClusterInfo) => o1.key.compareTo(o2.key)
  )
  override def creator: WorkerCollie.ClusterCreator =
    (_, creation) =>
      Future.successful(
        addCluster(
          key = creation.key,
          kind = ClusterStatus.Kind.WORKER,
          nodeNames = creation.nodeNames ++ clusterCache.asScala
            .find(_._1 == creation.key)
            .map(_._2.nodeNames)
            .getOrElse(Set.empty),
          imageName = creation.imageName,
          ports = creation.ports
        )
      )

  override def workerClient(
    cluster: WorkerClusterInfo
  )(implicit executionContext: ExecutionContext): Future[WorkerClient] =
    if (wkConnectionProps != null)
      Future.successful(
        WorkerClient.builder.workerClusterKey(ObjectKey.of("fake", "fake")).connectionProps(wkConnectionProps).build
      )
    else if (clusterCache.keySet().asScala.contains(cluster.key)) {
      val fake = FakeWorkerClient()
      val r    = fakeClientCache.putIfAbsent(cluster, fake)
      Future.successful(if (r == null) fake else r)
    } else
      Future.failed(new NoSuchClusterException(s"cluster:${cluster.key} is not running"))
}
