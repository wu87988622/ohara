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

import com.island.ohara.common.util.CommonUtils
import spray.json.DefaultJsonProtocol._
import spray.json.{DeserializationException, JsObject, JsString, JsValue, RootJsonFormat}

/**
  * we are about to live in a new world full of group and name. The pure string in the past is deprecated. Now, a whole
  * new class called DataKey is used to destroy the old world and lead us to enter a beautiful new world.
  *
  * NOTED: In order to accept the newbie who don't have group, the default value to group could be Data.GROUP_DEFAULT.
  *
  * NOTED: the group and name make up the primary key of a storable object (we call Data) to ohara.
  * @param group group
  * @param name name
  */
case class DataKey(group: String, name: String)

object DataKey {

  def apply(group: String, name: String): DataKey = new DataKey(
    group = CommonUtils.requireNonEmpty(group),
    name = CommonUtils.requireNonEmpty(name)
  )

  implicit val DATA_KEY_JSON_FORMAT: RootJsonFormat[DataKey] =
    JsonRefiner[DataKey]
      .format(new RootJsonFormat[DataKey] {
        override def read(json: JsValue): DataKey = try DataKey(
          group = noJsNull(json.asJsObject.fields)
            .get(Data.GROUP_KEY)
            .map(_.convertTo[String])
            .getOrElse(throw DeserializationException(s"where is your ${Data.GROUP_KEY} ???",
                                                      fieldNames = List(Data.GROUP_KEY))),
          name = noJsNull(json.asJsObject.fields)
            .get(Data.NAME_KEY)
            .map(_.convertTo[String])
            .getOrElse(
              throw DeserializationException(s"where is your ${Data.NAME_KEY} ???", fieldNames = List(Data.NAME_KEY))),
        )
        catch {
          case e: Throwable => throw DeserializationException("failed to generate DataKey from input json", e)
        }

        override def write(obj: DataKey): JsValue = JsObject(
          Data.GROUP_KEY -> JsString(obj.group),
          Data.NAME_KEY -> JsString(obj.name)
        )
      })
      .rejectEmptyString()
      .refine
}
