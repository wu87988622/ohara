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
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.common.annotations.{Optional, VisibleForTesting}
import com.island.ohara.common.cache.{Cache, RefreshableCache}
import com.island.ohara.common.util.Releasable

import scala.concurrent.duration._

trait MeterCache extends Releasable {
  def meters(clusterInfo: ClusterInfo): Map[String, Seq[Meter]]
}

object MeterCache {

  def builder(): Builder = new Builder()

  @VisibleForTesting
  private[store] case class RequestKey(name: String, service: String, clusterInfo: ClusterInfo) {
    override def equals(obj: Any): Boolean = obj match {
      case another: RequestKey => another.name == name && another.service == service
      case _                   => false
    }
    override def hashCode(): Int = 31 * name.hashCode + service.hashCode
  }

  class Builder {
    private[this] var fetcher: ClusterInfo => Map[String, Seq[Meter]] = _
    private[this] var refresher: () => Map[ClusterInfo, Map[String, Seq[Meter]]] = _
    private[this] var timeout: Duration = 5 seconds
    private[this] var frequency: Duration = timeout

    def fetcher(fetcher: ClusterInfo => Map[String, Seq[Meter]]): Builder = {
      this.fetcher = Objects.requireNonNull(fetcher)
      this
    }

    def refresher(refresher: () => Map[ClusterInfo, Map[String, Seq[Meter]]]): Builder = {
      this.refresher = Objects.requireNonNull(refresher)
      this
    }

    @Optional("default value is 5 seconds")
    def timeout(timeout: Duration): Builder = {
      this.timeout = Objects.requireNonNull(timeout)
      this.frequency = Objects.requireNonNull(timeout)
      this
    }

    @Optional("default value is equal to timeout")
    def frequency(frequency: Duration): Builder = {
      this.frequency = Objects.requireNonNull(frequency)
      this
    }

    def build(): MeterCache = new MeterCache {
      import scala.collection.JavaConverters._
      private[this] val fetcher = Objects.requireNonNull(Builder.this.fetcher)
      private[this] val refresher = Objects.requireNonNull(Builder.this.refresher)
      private[this] val timeout = Objects.requireNonNull(Builder.this.timeout)
      private[this] val closed = new AtomicBoolean(false)
      private[this] val cache = RefreshableCache
        .builder[RequestKey, Map[String, Seq[Meter]]]()
        .cache(
          Cache
            .builder[RequestKey, Map[String, Seq[Meter]]]()
            .fetcher(key => fetcher(key.clusterInfo))
            .timeout(java.time.Duration.ofMillis(timeout.toMillis))
            .build())
        .supplier(() =>
          refresher().map {
            case (clusterInfo, meters) =>
              key(clusterInfo) -> meters
          }.asJava)
        .frequency(java.time.Duration.ofMillis(frequency.toMillis))
        .build()

      private[this] def key(clusterInfo: ClusterInfo): RequestKey = RequestKey(
        name = clusterInfo.name,
        service = clusterInfo match {
          case _: BrokerClusterInfo => "bk"
          case _: WorkerClusterInfo => "wk"
          case c: ClusterInfo       => c.getClass.getSimpleName
        },
        clusterInfo = clusterInfo
      )

      override def meters(clusterInfo: ClusterInfo): Map[String, Seq[Meter]] = cache.get(key(clusterInfo))

      override def close(): Unit = if (closed.compareAndSet(false, true)) Releasable.close(cache)
    }
  }
}
