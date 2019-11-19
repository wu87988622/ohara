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

package com.island.ohara.configurator.store

import java.util.Objects
import java.util.concurrent.atomic.AtomicBoolean

import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.ClusterInfo
import com.island.ohara.client.configurator.v0.MetricsApi.Meter
import com.island.ohara.client.configurator.v0.StreamApi.StreamClusterInfo
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo
import com.island.ohara.common.annotations.{Optional, VisibleForTesting}
import com.island.ohara.common.cache.RefreshableCache
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.common.util.Releasable

import scala.concurrent.duration._

trait MeterCache extends Releasable {
  def meters(clusterInfo: ClusterInfo): Map[String, Seq[Meter]]
}

object MeterCache {
  def builder: Builder = new Builder()

  // TODO: remove this workaround if google guava support the custom comparison ... by chia
  @VisibleForTesting
  private[store] case class RequestKey(key: ObjectKey, service: String) {
    override def equals(obj: Any): Boolean = obj match {
      case another: RequestKey => another.key == key && another.service == service
      case _                   => false
    }
    override def hashCode(): Int  = 31 * key.hashCode + service.hashCode
    override def toString: String = s"key:$key, service:$service"
  }

  class Builder private[MeterCache] extends com.island.ohara.common.pattern.Builder[MeterCache] {
    private[this] var refresher: () => Map[ClusterInfo, Map[String, Seq[Meter]]] = _
    private[this] var frequency: Duration                                        = 5 seconds

    def refresher(refresher: () => Map[ClusterInfo, Map[String, Seq[Meter]]]): Builder = {
      this.refresher = Objects.requireNonNull(refresher)
      this
    }

    @Optional("default value is equal to timeout")
    def frequency(frequency: Duration): Builder = {
      this.frequency = Objects.requireNonNull(frequency)
      this
    }

    override def build: MeterCache = new MeterCache {
      import scala.collection.JavaConverters._
      private[this] val refresher = Objects.requireNonNull(Builder.this.refresher)
      private[this] val closed    = new AtomicBoolean(false)
      private[this] val cache = RefreshableCache
        .builder[RequestKey, Map[String, Seq[Meter]]]()
        .supplier(
          () =>
            refresher().map {
              case (clusterInfo, meters) =>
                key(clusterInfo) -> meters
            }.asJava
        )
        .frequency(java.time.Duration.ofMillis(frequency.toMillis))
        .build()

      private[this] def key(clusterInfo: ClusterInfo): RequestKey = RequestKey(
        key = clusterInfo.key,
        service = clusterInfo match {
          case _: ZookeeperClusterInfo => "zk"
          case _: BrokerClusterInfo    => "bk"
          case _: WorkerClusterInfo    => "wk"
          case _: StreamClusterInfo    => "stream"
          case c: ClusterInfo          => c.getClass.getSimpleName
        }
      )

      override def meters(clusterInfo: ClusterInfo): Map[String, Seq[Meter]] =
        cache.get(key(clusterInfo)).orElse(Map.empty)

      override def close(): Unit = if (closed.compareAndSet(false, true)) Releasable.close(cache)
    }
  }
}
