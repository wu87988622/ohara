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
import java.util.Objects

import com.island.ohara.common.annotations.Optional
import com.island.ohara.common.util.CommonUtils
import spray.json.DefaultJsonProtocol._
import spray.json.{DeserializationException, JsArray, JsNull, JsNumber, JsObject, JsString, JsValue, RootJsonFormat}

import scala.concurrent.{ExecutionContext, Future}

object PipelineApi {
  val PIPELINES_PREFIX_PATH: String = "pipelines"

  final case class Flow(from: String, to: Set[String])
  implicit val FLOW_JSON_FORMAT: RootJsonFormat[Flow] =
    JsonRefiner[Flow].format(jsonFormat2(Flow)).rejectEmptyString().refine

  /**
    * @param flows  this filed is declared as option type since ohara supports partial update. Empty array means you want to **cleanup** this
    *               field. And none means you don't want to change any bit of this field.
    */
  final case class Update(workerClusterName: Option[String], flows: Option[Seq[Flow]])

  implicit val PIPELINE_UPDATE_JSON_FORMAT: RootJsonFormat[Update] = JsonRefiner[Update]
    .format(new RootJsonFormat[Update] {
      private[this] val workerClusterNameKey = "workerClusterName"
      private[this] val flowsKey = "flows"
      private[this] val rulesKey = "rules"
      override def read(json: JsValue): Update = Update(
        workerClusterName = json.asJsObject.fields
          .get(workerClusterNameKey)
          // filter JsNULL
          .filter(_.isInstanceOf[JsString])
          .map(_.asInstanceOf[JsString].value),
        flows =
          if (json.asJsObject.fields.contains(flowsKey))
            json.asJsObject.fields(flowsKey) match {
              case JsNull     => None
              case a: JsArray => Some(a.elements.map(FLOW_JSON_FORMAT.read))
              case _          => throw DeserializationException(s"$flowsKey should be associated to array type")
            } else if (json.asJsObject.fields.contains(rulesKey))
            json.asJsObject.fields(rulesKey) match {
              case JsNull => None
              case o: JsObject =>
                Some(toFlows(o.fields.map {
                  case (k, v) => k -> v.asInstanceOf[JsArray].elements.map(_.asInstanceOf[JsString].value).toSet
                }))
              case _ => throw DeserializationException(s"$rulesKey should be associated to object type")
            } else None
      )

      override def write(obj: Update): JsValue = JsObject(
        Map(
          workerClusterNameKey -> obj.workerClusterName.map(JsString(_)).getOrElse(JsNull),
          flowsKey -> obj.flows.map(_.map(FLOW_JSON_FORMAT.write).toVector).map(JsArray(_)).getOrElse(JsNull),
          rulesKey -> obj.flows
            .map(_.map(e => e.from -> JsArray(e.to.map(JsString(_)).toVector)).toMap)
            .map(JsObject(_))
            .getOrElse(JsNull)
        ).filter {
          case (_, v) =>
            v match {
              case JsNull => false
              case _      => true
            }
        }
      )
    })
    .rejectEmptyString()
    .refine

  final case class Creation(name: String, workerClusterName: Option[String], flows: Seq[Flow]) extends CreationRequest {
    def rules: Map[String, Set[String]] = flows.map { flow =>
      flow.from -> flow.to
    }.toMap
  }

  private[this] def toFlows(rules: Map[String, Set[String]]): Seq[Flow] = rules.map { e =>
    Flow(
      from = e._1,
      to = e._2
    )
  }.toSeq

  object Creation {

    def apply(name: String, workerClusterName: Option[String], rules: Map[String, Set[String]]): Creation = Creation(
      name = name,
      workerClusterName = workerClusterName,
      flows = toFlows(rules)
    )
  }
  implicit val PIPELINE_CREATION_JSON_FORMAT: RootJsonFormat[Creation] = JsonRefiner[Creation]
    .format(new RootJsonFormat[Creation] {
      private[this] val nameKey = "name"
      private[this] val workerClusterNameKey = "workerClusterName"
      private[this] val flowsKey = "flows"
      private[this] val rulesKey = "rules"
      override def read(json: JsValue): Creation = {
        // reuse the code of paring update
        val update = PIPELINE_UPDATE_JSON_FORMAT.read(json)
        Creation(
          name = json.asJsObject.fields(nameKey).asInstanceOf[JsString].value,
          workerClusterName = update.workerClusterName,
          // TODO: we should reuse the JsonRefiner#nullToEmptyArray. However, we have to support the stale key "rules" ...
          flows = update.flows.getOrElse(Seq.empty)
        )
      }

      override def write(obj: Creation): JsValue = JsObject(
        nameKey -> JsString(obj.name),
        workerClusterNameKey -> obj.workerClusterName.map(JsString(_)).getOrElse(JsNull),
        flowsKey -> JsArray(obj.flows.map(FLOW_JSON_FORMAT.write).toVector),
        rulesKey -> JsObject(obj.rules.map { e =>
          e._1 -> JsArray(e._2.map(JsString(_)).toVector)
        })
      )
    })
    .rejectEmptyString()
    .refine

  import MetricsApi._

  final case class ObjectAbstract(id: String,
                                  name: String,
                                  kind: String,
                                  className: Option[String],
                                  state: Option[String],
                                  error: Option[String],
                                  metrics: Metrics,
                                  lastModified: Long)
      extends Data
  implicit val OBJECT_ABSTRACT_JSON_FORMAT: RootJsonFormat[ObjectAbstract] = jsonFormat8(ObjectAbstract)

  final case class Pipeline(name: String,
                            flows: Seq[Flow],
                            objects: Seq[ObjectAbstract],
                            workerClusterName: String,
                            lastModified: Long)
      extends Data {
    override def id: String = name
    override def kind: String = "pipeline"
    def rules: Map[String, Set[String]] = flows.map { flow =>
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

  /**
    * used to generate the payload and url for POST/PUT request.
    */
  trait Request {
    def name(name: String): Request
    @Optional("server will match a broker cluster for you if the wk name is ignored")
    def workerClusterName(workerClusterName: String): Request
    @Optional("default value is empty")
    def flows(flows: Seq[Flow]): Request
    @Optional("default value is empty")
    def flow(from: String, to: String): Request = flow(from, Set(to))
    @Optional("default value is empty")
    def flow(from: String, to: Set[String]): Request = flow(Flow(from = from, to = to))
    @Optional("default value is empty")
    def flow(flow: Flow): Request = flows(Seq(Objects.requireNonNull(flow)))

    /**
      * generate the POST request
      * @param executionContext thread pool
      * @return created data
      */
    def create()(implicit executionContext: ExecutionContext): Future[Pipeline]

    /**
      * generate the PUT request
      * @param executionContext thread pool
      * @return updated/created data
      */
    def update()(implicit executionContext: ExecutionContext): Future[Pipeline]
  }

  class Access private[v0] extends Access2[Pipeline](PIPELINES_PREFIX_PATH) {
    def request: Request = new Request {
      private[this] var name: String = _
      private[this] var workerClusterName: Option[String] = None
      private[this] var flows: Seq[Flow] = _
      override def name(name: String): Request = {
        this.name = CommonUtils.requireNonEmpty(name)
        this
      }

      override def workerClusterName(workerClusterName: String): Request = {
        this.workerClusterName = Some(CommonUtils.requireNonEmpty(workerClusterName))
        this
      }

      override def flows(flows: Seq[Flow]): Request = {
        this.flows = Objects.requireNonNull(flows)
        this
      }

      override def create()(implicit executionContext: ExecutionContext): Future[Pipeline] =
        exec.post[Creation, Pipeline, ErrorApi.Error](
          _url,
          Creation(
            name = CommonUtils.requireNonEmpty(name),
            workerClusterName = workerClusterName,
            flows = if (flows == null) Seq.empty else flows
          )
        )
      override def update()(implicit executionContext: ExecutionContext): Future[Pipeline] =
        exec.put[Update, Pipeline, ErrorApi.Error](
          s"${_url}/${CommonUtils.requireNonEmpty(name)}",
          Update(
            workerClusterName = workerClusterName,
            flows = Option(flows)
          )
        )
    }
  }

  def access: Access = new Access
}
