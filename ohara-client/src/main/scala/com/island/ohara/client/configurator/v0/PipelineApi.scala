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
import com.island.ohara.client.kafka.Enum
import spray.json.DefaultJsonProtocol._
import spray.json.{JsString, JsValue, RootJsonFormat}

object PipelineApi {
  val UNKNOWN: String = "?"
  val PIPELINES_PREFIX_PATH: String = "pipelines"

  /**
    * Represent an "interface" for all kinds of state.
    * This is the top level class if there was any other kinds of state that are used in the pipeline.
    * TODO : "Pipeline API" should only see the base states (ex: RUNNING, STOP, FAILED), we could abstract these states
    * and move to pipeline level "pipelineState"...by Sam
    *
    * @param name the actual name of the sate
    */
  abstract sealed class ObjectState { val name: String }
  object ObjectState extends Enum[ObjectState] {
    case object UNKNOWN extends ObjectState { val name: String = "UNKNOWN" }
    case object SUCCEEDED extends ObjectState { val name: String = "SUCCEEDED" }
    case object UNASSIGNED extends ObjectState { val name: String = "UNASSIGNED" }
    case object RUNNING extends ObjectState { val name: String = "RUNNING" }
    case object PAUSED extends ObjectState { val name: String = "PAUSED" }
    case object PENDING extends ObjectState { val name: String = "PENDING" }
    case object FAILED extends ObjectState { val name: String = "FAILED" }
    case object DESTROYED extends ObjectState { val name: String = "DESTROYED" }
    case object CREATED extends ObjectState { val name: String = "CREATED" }
    case object RESTARTING extends ObjectState { val name: String = "RESTARTING" }
    case object REMOVING extends ObjectState { val name: String = "REMOVING" }
    case object EXITED extends ObjectState { val name: String = "EXITED" }
    case object DEAD extends ObjectState { val name: String = "DEAD" }
  }

  implicit val OBJECT_STATE_JSON_FORMAT: RootJsonFormat[ObjectState] =
    new RootJsonFormat[ObjectState] {
      override def write(obj: ObjectState): JsValue = JsString(obj.name)
      override def read(json: JsValue): ObjectState = ObjectState.forName(json.asInstanceOf[JsString].value)
    }

  final case class PipelineCreationRequest(name: String,
                                           workerClusterName: Option[String],
                                           rules: Map[String, Seq[String]])
  implicit val PIPELINE_REQUEST_JSON_FORMAT: RootJsonFormat[PipelineCreationRequest] = jsonFormat3(
    PipelineCreationRequest)

  final case class ObjectAbstract(id: String,
                                  name: String,
                                  kind: String,
                                  state: Option[ObjectState],
                                  error: Option[String],
                                  lastModified: Long)
      extends Data
  implicit val OBJECT_ABSTRACT_JSON_FORMAT: RootJsonFormat[ObjectAbstract] = jsonFormat6(ObjectAbstract)

  final case class Pipeline(id: String,
                            name: String,
                            rules: Map[String, Seq[String]],
                            objects: Seq[ObjectAbstract],
                            workerClusterName: String,
                            lastModified: Long)
      extends Data {
    override def kind: String = "pipeline"
  }
  implicit val PIPELINE_JSON_FORMAT: RootJsonFormat[Pipeline] = jsonFormat6(Pipeline)

  def access(): Access[PipelineCreationRequest, Pipeline] =
    new Access[PipelineCreationRequest, Pipeline](PIPELINES_PREFIX_PATH)
}
