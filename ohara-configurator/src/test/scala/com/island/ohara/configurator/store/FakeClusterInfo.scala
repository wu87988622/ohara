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

import com.island.ohara.client.configurator.v0.MetricsApi.Metrics
import com.island.ohara.client.configurator.v0.{ClusterInfo, MetricsApi}
import com.island.ohara.common.util.CommonUtils
import spray.json.JsValue

case class FakeClusterInfo(name: String) extends ClusterInfo {

  override def imageName: String = "I DON'T CARE"

  override def ports: Set[Int] = Set.empty

  override def nodeNames: Set[String] = Set.empty

  override def deadNodes: Set[String] = Set.empty

  override def group: String = "fake_group"

  override def lastModified: Long = CommonUtils.current()

  override def kind: String = "fake_cluster"

  override def tags: Map[String, JsValue] = Map.empty

  override def state: Option[String] = None

  override def error: Option[String] = None

  override def metrics: MetricsApi.Metrics = Metrics.EMPTY

  override def settings: Map[String, JsValue] = throw new UnsupportedOperationException
}
