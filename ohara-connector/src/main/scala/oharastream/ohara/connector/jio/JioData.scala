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

package oharastream.ohara.connector.jio

import oharastream.ohara.common.data.Row
import spray.json.{JsNull, JsObject, JsValue, RootJsonFormat}
class JioData private (raw: Map[String, JsValue]) {
  def fields: Map[String, JsValue] = raw.filter {
    case (_, value) =>
      value match {
        case JsNull => false
        case _      => true
      }
  }

  /**
    * convert this jio data to row.
    * @return row
    */
  def row: Row = toRow(JsObject(fields))

  override def equals(o: Any): Boolean = o match {
    case other: JioData => other.fields == fields
    case _              => false
  }

  override def hashCode(): Int  = fields.hashCode()
  override def toString: String = fields.toString()
}

object JioData {
  def apply(raw: Map[String, JsValue]): JioData = new JioData(raw)

  def apply(row: Row): JioData = apply(toJson(row).fields)

  implicit val JIO_DATA_FORMAT: RootJsonFormat[JioData] = new RootJsonFormat[JioData] {
    override def write(obj: JioData): JsValue = JsObject(obj.fields)
    override def read(json: JsValue): JioData = JioData(json.asJsObject.fields)
  }
}
