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

import java.util.Objects

import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.ClusterInfo
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo
import com.island.ohara.common.annotations.{Optional, VisibleForTesting}
import com.island.ohara.common.cache.RefreshableCache
import com.island.ohara.common.util.Releasable

import scala.collection.JavaConverters._
import scala.concurrent.duration.{Duration, _}

/**
  * The cost of fetching containers via ssh is too expensive, and our ssh collie almost die for it. A quick solution
  * is a simple cache which storing the cluster information in local memory. The cache shines at getter method however
  * the side-effect is that the stuff from getter may be out-of-date. But, in most use cases we bear it well.
  */
trait ClusterCache extends Releasable {

  /**
    * @return the cached data
    */
  def snapshot: Map[ClusterInfo, Seq[ContainerInfo]]

  /**
    * The inner time-based auto-refresher is enough to most use cases. However, we are always in a situation that we should
    * update the cache right now. This method save your life that you can request the inner thread to update the cache.
    * Noted, the method doesn't block your thread since what it does is to send a request without any wait.
    */
  def requestUpdate(): Unit
}

object ClusterCache {

  def builder(): Builder = new Builder

  // TODO: remove this workaround if google guava support the custom comparison ... by chia
  @VisibleForTesting
  private[agent] case class RequestKey(name: String, service: String, clusterInfo: ClusterInfo) {
    override def equals(obj: Any): Boolean = obj match {
      case another: RequestKey => another.name == name && another.service == service
      case _                   => false
    }
    override def hashCode(): Int = 31 * name.hashCode + service.hashCode
  }

  class Builder private[ClusterCache] {
    private[this] var frequency: Duration = 3 seconds
    private[this] var supplier: () => Map[ClusterInfo, Seq[ContainerInfo]] = _

    @Optional("default value is 3 seconds")
    def frequency(frequency: Duration): Builder = {
      this.frequency = Objects.requireNonNull(frequency)
      this
    }

    /**
      * the function to supply all data to cache.
      * @param supplier supplier
      * @return this builder
      */
    def supplier(supplier: () => Map[ClusterInfo, Seq[ContainerInfo]]): Builder = {
      this.supplier = Objects.requireNonNull(supplier)
      this
    }

    private[this] def checkArguments(): Unit = {
      Objects.requireNonNull(frequency)
      Objects.requireNonNull(supplier)
    }

    def build(): ClusterCache = {
      checkArguments()
      new ClusterCache {
        private[this] val cache = RefreshableCache
          .builder[RequestKey, Seq[ContainerInfo]]()
          .supplier(() =>
            supplier().map {
              case (clusterInfo, containers) => key(clusterInfo) -> containers
            }.asJava)
          .frequency(java.time.Duration.ofMillis(frequency.toMillis))
          .build()

        override def close(): Unit = Releasable.close(cache)

        override def snapshot: Map[ClusterInfo, Seq[ContainerInfo]] = cache.snapshot().asScala.toMap.map {
          case (key, containers) => key.clusterInfo -> containers
        }

        override def requestUpdate(): Unit = cache.requestUpdate()

        private[this] def key(clusterInfo: ClusterInfo): RequestKey = RequestKey(
          name = clusterInfo.name,
          service = clusterInfo match {
            case _: ZookeeperClusterInfo => "zk"
            case _: BrokerClusterInfo    => "bk"
            case _: WorkerClusterInfo    => "wk"
            case c: ClusterInfo          => c.getClass.getSimpleName
          },
          clusterInfo = clusterInfo
        )
      }
    }
  }
}
