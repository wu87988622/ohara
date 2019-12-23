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
import com.island.ohara.client.configurator.v0.StreamApi.StreamClusterInfo
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterInfo
import com.island.ohara.common.setting.ObjectKey
import spray.json.{JsArray, JsString, JsValue}
import spray.json._
import spray.json.DefaultJsonProtocol._

/**
  * There are many kinds of cluster hosted by ohara. We extract an interface to define "what" information should be included by a "cluster
  * information".
  */
trait ClusterInfo extends Data {
  /**
    * @return the settings to set up this cluster. This is the raw data of settings.
    */
  def settings: Map[String, JsValue]

  override def group: String = noJsNull(settings)(GROUP_KEY).convertTo[String]

  override def name: String = noJsNull(settings)(NAME_KEY).convertTo[String]

  /**
    * override the key to avoid conflict of double inheritance.
    */
  override def key: ObjectKey = ObjectKey.of(group, name)

  /**
    * @return docker image name used to build container for this cluster
    */
  def imageName: String = noJsNull(settings)(IMAGE_NAME_KEY).convertTo[String]

  /**
    * All services hosted by ohara should use some ports, which are used to communicate.
    * We "highlight" this method since port checking is a important thing for configurator
    * @return ports used by this cluster
    */
  def ports: Set[Int]

  /**
    * the port used to expose the jmx service
    * @return jmx port
    */
  def jmxPort: Int = noJsNull(settings)(JMX_PORT_KEY).convertTo[Int]

  /**
    * @return nodes running this cluster
    */
  def nodeNames: Set[String] = noJsNull(settings)(NODE_NAMES_KEY).convertTo[Set[String]]

  /**
    * @return the state of this cluster. None means the cluster is not running
    */
  def state: Option[String]

  /**
    * @return the error message of the dead cluster
    */
  def error: Option[String]

  /**
    * the nodes do run the containers of cluster.
    * @return a collection of node names
    */
  def aliveNodes: Set[String]

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

  /**
    * the default comparison for all clusters.
    */
  override protected def matched(key: String, value: String): Boolean = key match {
    case "state" => matchOptionString(state, value)
    case "aliveNodes" =>
      value.parseJson match {
        case JsArray(es) =>
          if (es.forall(_.isInstanceOf[JsString])) es.map(_.convertTo[String]).toSet == aliveNodes
          else false
        case _ => false
      }
    case _ => matchSetting(settings, key, value)
  }

  /**
    * @return size (in MB) of init heap
    */
  def initHeap: Int = noJsNull(settings)(INIT_HEAP_KEY).convertTo[Int]

  /**
    * @return size (in MB) of max heap
    */
  def maxHeap: Int = noJsNull(settings)(MAX_HEAP_KEY).convertTo[Int]

  def tags: Map[String, JsValue] = noJsNull(settings)(TAGS_KEY).asJsObject.fields
}
