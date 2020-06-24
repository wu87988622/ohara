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

import oharastream.ohara.client.Enum
import oharastream.ohara.common.setting.SettingDef
import spray.json.DefaultJsonProtocol._
import spray.json.{JsString, JsValue, RootJsonFormat}

object VolumeApi {
  val KIND: String = SettingDef.Reference.VOLUME.name().toLowerCase

  final case class Creation(
    override val group: String,
    override val name: String,
    nodeNames: Set[String],
    path: String,
    override val tags: Map[String, JsValue]
  ) extends BasicCreation {
    override def raw: Map[String, JsValue] = CREATION_FORMAT.write(this).asJsObject.fields
  }
  implicit val CREATION_FORMAT: JsonRefiner[Creation] =
    rulesOfKey[Creation]
      .format(jsonFormat5(Creation))
      .rejectEmptyArray()
      .nullToEmptyObject(TAGS_KEY)
      .build

  final case class Updating(override val tags: Option[Map[String, JsValue]]) extends BasicUpdating {
    override def raw: Map[String, JsValue] = UPDATING_FORMAT.write(this).asJsObject.fields
  }

  implicit val UPDATING_FORMAT: RootJsonFormat[Updating] =
    JsonRefinerBuilder[Updating].format(jsonFormat1(Updating)).build

  abstract sealed class VolumeState(val name: String) extends Serializable
  object VolumeState extends Enum[VolumeState] {
    case object RUNNING extends VolumeState("RUNNING")
  }

  implicit val VOLUME_STATE_FORMAT: RootJsonFormat[VolumeState] = new RootJsonFormat[VolumeState] {
    override def read(json: JsValue): VolumeState = VolumeState.forName(json.convertTo[String].toUpperCase)
    override def write(obj: VolumeState): JsValue = JsString(obj.name)
  }

  final case class Volume(
    override val group: String,
    override val name: String,
    nodeNames: Set[String],
    path: String,
    state: Option[VolumeState],
    error: Option[String],
    override val tags: Map[String, JsValue],
    override val lastModified: Long
  ) extends Data {
    override def kind: String = KIND

    override def raw: Map[String, JsValue] = VOLUME_FORMAT.write(this).asJsObject.fields
  }

  implicit val VOLUME_FORMAT: RootJsonFormat[Volume] =
    rulesOfKey[Volume]
      .format(jsonFormat8(Volume))
      .rejectEmptyArray("nodeNames")
      .nullToEmptyObject(TAGS_KEY)
      .build
}
