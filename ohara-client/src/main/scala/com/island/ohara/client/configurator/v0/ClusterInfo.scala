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

package com.island.ohara.client.configurator.v0

import com.island.ohara.client.configurator.Data
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterInfo
import com.island.ohara.client.configurator.v0.MetricsApi.Metrics
import com.island.ohara.client.configurator.v0.StreamApi.StreamClusterInfo
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo
import spray.json.JsValue

/**
  * There are many kinds of cluster hosted by ohara. We extract an interface to define "what" information should be included by a "cluster
  * information".
  */
trait ClusterInfo extends Data {

  /**
    * @return docker image name used to build container for this cluster
    */
  def imageName: String

  /**
    * All services hosted by ohara should use some ports, which are used to communicate.
    * We "highlight" this method since port checking is a important thing for configurator
    * @return ports used by this cluster
    */
  def ports: Set[Int]

  /**
    * @return nodes running this cluster
    */
  def nodeNames: Set[String]

  /**
    * List the dead nodes. the number of dead nodes is calculated since the gone container may be not leave any information
    * to us to trace. By contrast, the number of alive nodes is real since we can observe the "running" state from the
    * nodes.
    *
    * @return nothing if there is no state (normally, it means there is no containers on the nodes). otherwise, the number
    *         of dead nodes is equal to (the number of node names) - (the number of alive nodes)
    */
  def deadNodes: Set[String] = if (state.isEmpty) Set.empty else nodeNames -- aliveNodes

  /**
    * @return the state of this cluster. None means the cluster is not running
    */
  def state: Option[String]

  /**
    * the nodes do run the containers of cluster.
    * @return a collection of node names
    */
  def aliveNodes: Set[String]

  /**
    * @return the error message of this cluster.
    */
  def error: Option[String]

  /**
    * the metrics of this cluster. Noted that only stream cluster is able to fetch the metrics.
    * @return metrics
    */
  def metrics: Metrics

  /**
    * @return the settings to set up this cluster. This is the raw data of settings.
    */
  def settings: Map[String, JsValue]

  /**
    * this is a small helper method used to update the node names for cluster info
    * @return updated cluster info
    */
  def newNodeNames(newNodeNames: Set[String]): ClusterInfo = this match {
    case c: ZookeeperClusterInfo =>
      c.copy(settings = ZookeeperApi.access.request.settings(settings).nodeNames(newNodeNames).creation.settings)
    case c: BrokerClusterInfo =>
      c.copy(settings = BrokerApi.access.request.settings(settings).nodeNames(newNodeNames).creation.settings)
    case c: WorkerClusterInfo =>
      c.copy(settings = WorkerApi.access.request.settings(settings).nodeNames(newNodeNames).creation.settings)
    case c: StreamClusterInfo =>
      c.copy(settings = StreamApi.access.request.settings(settings).nodeNames(newNodeNames).creation.settings)
  }
}
