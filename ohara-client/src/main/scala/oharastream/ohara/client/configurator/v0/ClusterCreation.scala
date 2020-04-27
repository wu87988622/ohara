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

package oharastream.ohara.client.configurator.v0

import oharastream.ohara.common.setting.ObjectKey
import spray.json.DefaultJsonProtocol._
import spray.json.{JsString, JsValue}

/**
  * this is a basic interface of cluster request to create a normal object resource.
  */
trait ClusterCreation extends BasicCreation {
  /**
    * @return the raw settings from request
    */
  def settings: Map[String, JsValue]

  def group: String = noJsNull(settings)(GROUP_KEY).convertTo[String]

  def name: String = noJsNull(settings)(NAME_KEY).convertTo[String]

  /**
    * @return nodes to run this cluster
    */
  def nodeNames: Set[String] = noJsNull(settings)(NODE_NAMES_KEY).convertTo[Set[String]]

  /**
    * @return image used to build cluster
    */
  def imageName: String = noJsNull(settings)(IMAGE_NAME_KEY).convertTo[String]

  /**
    * @return ports used by this cluster
    */
  def ports: Set[Int]

  /**
    * the port used to expose the jmx service
    * @return jmx port
    */
  def jmxPort: Int = noJsNull(settings)(JMX_PORT_KEY).convertTo[Int]

  def routes: Map[String, String] =
    noJsNull(settings)(ROUTES_KEY).asJsObject.fields.filter(_._2.isInstanceOf[JsString]).map {
      case (k, v) => k -> v.convertTo[String]
    }

  def tags: Map[String, JsValue] = noJsNull(settings)(TAGS_KEY).asJsObject.fields

  def maxHeap: Long = noJsNull(settings)(MAX_HEAP_KEY).convertTo[Long]

  def initHeap: Long = noJsNull(settings)(INIT_HEAP_KEY).convertTo[Long]

  /**
    * @return the volume object key and related container path. For example, "{"group":"a", "name": "b"}" -> "/tmp/aaa" means the
    *         object key "{"group":"a", "name": "b"}" is mapped to a volume and the volume should be mounted on
    *         container's "/tmp/aaa"
    */
  def volumeMaps: Map[ObjectKey, String] = Map.empty
}
