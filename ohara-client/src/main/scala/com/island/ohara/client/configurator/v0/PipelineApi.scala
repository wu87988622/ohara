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
import spray.json.{JsArray, JsNull, JsNumber, JsObject, JsString, JsValue, RootJsonFormat}

object PipelineApi {

  /**
    * this constant represents the "unknown" from or "unknown" to.
    */
  val UNKNOWN_ID: String = "?"
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

  final case class Flow(from: String, to: Seq[String])
  implicit val FLOW_JSON_FORMAT: RootJsonFormat[Flow] = jsonFormat2(Flow)

  final case class PipelineCreationRequest(name: String, workerClusterName: Option[String], flows: Seq[Flow]) {
    def rules: Map[String, Seq[String]] = flows.map { flow =>
      flow.from -> flow.to
    }.toMap
  }

  def toFlows(rules: Map[String, Seq[String]]): Seq[Flow] = rules.map { e =>
    Flow(
      from = e._1,
      to = e._2
    )
  }.toSeq

  object PipelineCreationRequest {

    def apply(name: String,
              workerClusterName: Option[String],
              rules: Map[String, Seq[String]]): PipelineCreationRequest = PipelineCreationRequest(
      name = name,
      workerClusterName = workerClusterName,
      flows = toFlows(rules)
    )
  }
  implicit val PIPELINE_REQUEST_JSON_FORMAT: RootJsonFormat[PipelineCreationRequest] =
    new RootJsonFormat[PipelineCreationRequest] {
      private[this] val nameKey = "name"
      private[this] val workerClusterNameKey = "workerClusterName"
      private[this] val flowsKey = "flows"
      private[this] val rulesKey = "rules"
      override def read(json: JsValue): PipelineCreationRequest = PipelineCreationRequest(
        name = json.asJsObject.fields(nameKey).asInstanceOf[JsString].value,
        workerClusterName = json.asJsObject.fields
          .get(workerClusterNameKey)
          // filter JsNULL
          .filter(_.isInstanceOf[JsString])
          .map(_.asInstanceOf[JsString].value),
        flows = json.asJsObject.fields
          .get(flowsKey)
          .map(_.asInstanceOf[JsArray].elements.map(FLOW_JSON_FORMAT.read).toSeq)
          .getOrElse(
            toFlows(
              json.asJsObject.fields
                .get(rulesKey)
                .map(_.asInstanceOf[JsObject].fields.map {
                  case (k, v) => k -> v.asInstanceOf[JsArray].elements.map(_.asInstanceOf[JsString].value)
                })
                .getOrElse(Map.empty))),
      )

      override def write(obj: PipelineCreationRequest): JsValue = JsObject(
        nameKey -> JsString(obj.name),
        workerClusterNameKey -> obj.workerClusterName.map(JsString(_)).getOrElse(JsNull),
        flowsKey -> JsArray(obj.flows.map(FLOW_JSON_FORMAT.write).toVector),
        rulesKey -> JsObject(obj.rules.map { e =>
          e._1 -> JsArray(e._2.map(JsString(_)).toVector)
        })
      )
    }

  final case class ObjectAbstract(id: String,
                                  name: String,
                                  kind: String,
                                  className: Option[String],
                                  state: Option[ObjectState],
                                  error: Option[String],
                                  lastModified: Long)
      extends Data
  implicit val OBJECT_ABSTRACT_JSON_FORMAT: RootJsonFormat[ObjectAbstract] = jsonFormat7(ObjectAbstract)

  final case class Pipeline(id: String,
                            name: String,
                            flows: Seq[Flow],
                            objects: Seq[ObjectAbstract],
                            workerClusterName: String,
                            lastModified: Long)
      extends Data {
    override def kind: String = "pipeline"
    def rules: Map[String, Seq[String]] = flows.map { flow =>
      flow.from -> flow.to
    }.toMap
  }
  implicit val PIPELINE_JSON_FORMAT: RootJsonFormat[Pipeline] = new RootJsonFormat[Pipeline] {
    private[this] val idKey = "id"
    private[this] val nameKey = "name"
    private[this] val workerClusterNameKey = "workerClusterName"
    private[this] val flowsKey = "flows"
    private[this] val rulesKey = "rules"
    private[this] val objectsKey = "objects"
    private[this] val lastModifiedKey = "lastModified"
    override def read(json: JsValue): Pipeline = Pipeline(
      id = json.asJsObject.fields(idKey).asInstanceOf[JsString].value,
      name = json.asJsObject.fields(nameKey).asInstanceOf[JsString].value,
      workerClusterName = json.asJsObject.fields(workerClusterNameKey).asInstanceOf[JsString].value,
      flows = json.asJsObject.fields
        .get(flowsKey)
        .map(_.asInstanceOf[JsArray].elements.map(FLOW_JSON_FORMAT.read).toSeq)
        .getOrElse(Seq.empty),
      objects = json.asJsObject.fields(objectsKey).asInstanceOf[JsArray].elements.map(OBJECT_ABSTRACT_JSON_FORMAT.read),
      lastModified = json.asJsObject.fields(lastModifiedKey).asInstanceOf[JsNumber].value.toLong
    )
    override def write(obj: Pipeline): JsValue = JsObject(
      idKey -> JsString(obj.id),
      nameKey -> JsString(obj.name),
      workerClusterNameKey -> JsString(obj.workerClusterName),
      flowsKey -> JsArray(obj.flows.map(FLOW_JSON_FORMAT.write).toVector),
      rulesKey -> JsObject(obj.rules.map { e =>
        e._1 -> JsArray(e._2.map(JsString(_)).toVector)
      }),
      objectsKey -> JsArray(obj.objects.map(OBJECT_ABSTRACT_JSON_FORMAT.write).toVector),
      lastModifiedKey -> JsNumber(obj.lastModified)
    )
  }

  def access(): Access[PipelineCreationRequest, Pipeline] =
    new Access[PipelineCreationRequest, Pipeline](PIPELINES_PREFIX_PATH)
}
