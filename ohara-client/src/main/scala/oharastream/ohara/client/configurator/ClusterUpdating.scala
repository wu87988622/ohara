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

package oharastream.ohara.client.configurator
import spray.json.DefaultJsonProtocol._
import spray.json.{JsString, JsValue}

/**
  * this is a basic interface of cluster request to update a normal object resource.
  */
trait ClusterUpdating {
  /**
    * @return the raw settings from request
    */
  def settings: Map[String, JsValue]

  def imageName: Option[String] = noJsNull(settings).get(IMAGE_NAME_KEY).map(_.convertTo[String])

  def nodeNames: Option[Set[String]] = noJsNull(settings).get(NODE_NAMES_KEY).map(_.convertTo[Seq[String]].toSet)

  def routes: Option[Map[String, String]] =
    noJsNull(settings)
      .get(ROUTES_KEY)
      .map(_.asJsObject.fields.filter(_._2.isInstanceOf[JsString]).map {
        case (k, v) => k -> v.convertTo[String]
      })

  def jmxPort: Option[Int] = noJsNull(settings).get(JMX_PORT_KEY).map(_.convertTo[Int])

  def tags: Option[Map[String, JsValue]] = noJsNull(settings).get(TAGS_KEY).map(_.asJsObject.fields)
}
