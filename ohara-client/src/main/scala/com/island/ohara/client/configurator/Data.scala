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

package com.island.ohara.client.configurator

import com.island.ohara.common.setting.ObjectKey
import spray.json.JsValue

/**
  * This is the basic type which can be stored by configurator.
  * All members are declared as "def" since not all subclasses intend to represent all members in restful APIs.
  */
trait Data {
  def group: String
  def name: String

  /**
    * a helper method used to generate the key of this data.
    * @return key
    */
  def key: ObjectKey = ObjectKey.of(group, name)

  def lastModified: Long
  def kind: String
  def tags: Map[String, JsValue]

  /**
    * compare the fields of sub instance.
    * @param key field name
    * @param value field value
    * @return true if it is matched. otherwise, false
    */
  protected def matched(key: String, value: String): Boolean = false

  import spray.json._

  /**
    * by default, the query compare only "name", "group", "tags" and lastModified.
    * @param request query request
    * @return true if the query matches this object. otherwise, false
    */
  final def matched(request: QueryRequest): Boolean =
    try request.raw.forall {
      case (key, value) =>
        key match {
          case "name" => value == name
          case "group" =>
            value == group
          case "tags"         => value.parseJson.asJsObject == JsObject(tags)
          case "lastModified" => value.toLong == lastModified

          /**
            * this field belongs to sub class so we count on the implementation of sub class.
            */
          case _ =>
            matched(key, value)
        }
    } catch {
      /**
        * this means the key is not a part of data :(
        */
      case _: MatchError => false
    }
}
