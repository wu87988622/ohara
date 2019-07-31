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
import com.island.ohara.kafka.connector.json.ObjectKey
import spray.json.DefaultJsonProtocol._
import spray.json.{DeserializationException, JsArray, JsNull, JsNumber, JsObject, JsString, JsValue, RootJsonFormat}

import scala.concurrent.{ExecutionContext, Future}

object PipelineApi {
  val PIPELINES_PREFIX_PATH: String = "pipelines"

  final case class Flow(from: ObjectKey, to: Set[ObjectKey])
  implicit val FLOW_JSON_FORMAT: OharaJsonFormat[Flow] =
    JsonRefiner[Flow].format(jsonFormat2(Flow)).rejectEmptyString().refine

  /**
    * @param flows  this filed is declared as option type since ohara supports partial update. Empty array means you want to **cleanup** this
    *               field. And none means you don't want to change any bit of this field.
    */
  final case class Update(workerClusterName: Option[String],
                          flows: Option[Seq[Flow]],
                          tags: Option[Map[String, JsValue]])

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
          .map(_.convertTo[String]),
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
                  case (k, v) => k -> v.asInstanceOf[JsArray].elements.map(_.convertTo[String]).toSet
                }))
              case _ => throw DeserializationException(s"$rulesKey should be associated to object type")
            } else None,
        tags = json.asJsObject.fields
          .get(Data.TAGS_KEY)
          .filter {
            case JsNull => false
            case _      => true
          }
          .map {
            case a: JsObject => a.fields
            case _           => throw DeserializationException(s"$flowsKey should be associated to JsObject type")
          }
      )

      override def write(obj: Update): JsValue = JsObject(
        Map(
          workerClusterNameKey -> obj.workerClusterName.map(JsString(_)).getOrElse(JsNull),
          flowsKey -> obj.flows.map(_.map(FLOW_JSON_FORMAT.write).toVector).map(JsArray(_)).getOrElse(JsNull),
          rulesKey -> obj.flows
            .map(_.map(e => e.from.name -> JsArray(e.to.map(_.name).map(JsString(_)).toVector)).toMap)
            .map(JsObject(_))
            .getOrElse(JsNull),
          Data.TAGS_KEY -> obj.tags.map(JsObject(_)).getOrElse(JsNull),
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

  final case class Creation(group: String,
                            name: String,
                            workerClusterName: Option[String],
                            flows: Seq[Flow],
                            tags: Map[String, JsValue])
      extends CreationRequest {
    def rules: Map[String, Set[String]] = flows.map { flow =>
      flow.from.name -> flow.to.map(_.name)
    }.toMap
  }

  private[this] def toFlows(rules: Map[String, Set[String]]): Seq[Flow] = rules.map { e =>
    Flow(
      from = ObjectKey.of(Data.GROUP_DEFAULT, e._1),
      to = e._2.map(ObjectKey.of(Data.GROUP_DEFAULT, _))
    )
  }.toSeq

  implicit val PIPELINE_CREATION_JSON_FORMAT: OharaJsonFormat[Creation] = JsonRefiner[Creation]
    .format(new RootJsonFormat[Creation] {
      private[this] val workerClusterNameKey = "workerClusterName"
      private[this] val flowsKey = "flows"
      private[this] val rulesKey = "rules"
      override def read(json: JsValue): Creation = {
        // reuse the code of paring update
        val update = PIPELINE_UPDATE_JSON_FORMAT.read(json)
        Creation(
          group = json.asJsObject.fields(Data.GROUP_KEY).convertTo[String],
          name = json.asJsObject.fields(Data.NAME_KEY).convertTo[String],
          workerClusterName = update.workerClusterName,
          // TODO: we should reuse the JsonRefiner#nullToEmptyArray. However, we have to support the stale key "rules" ...
          flows = update.flows.getOrElse(Seq.empty),
          tags = update.tags.getOrElse(Map.empty)
        )
      }

      override def write(obj: Creation): JsValue = JsObject(
        noJsNull(Map(
          Data.GROUP_KEY -> JsString(obj.group),
          Data.NAME_KEY -> JsString(obj.name),
          workerClusterNameKey -> obj.workerClusterName.map(JsString(_)).getOrElse(JsNull),
          flowsKey -> JsArray(obj.flows.map(FLOW_JSON_FORMAT.write).toVector),
          rulesKey -> JsObject(obj.rules.map { e =>
            e._1 -> JsArray(e._2.map(JsString(_)).toVector)
          }),
          Data.TAGS_KEY -> JsObject(obj.tags)
        ))
      )
    })
    .rejectEmptyString()
    .stringRestriction(Set(Data.GROUP_KEY, Data.NAME_KEY))
    .withNumber()
    .withCharset()
    .withDot()
    .withDash()
    .withUnderLine()
    .toRefiner
    .nullToString(Data.GROUP_KEY, () => Data.GROUP_DEFAULT)
    .nullToString(Data.NAME_KEY, () => CommonUtils.randomString(10))
    .nullToEmptyObject(Data.TAGS_KEY)
    .refine

  import MetricsApi._

  final case class ObjectAbstract(group: String,
                                  name: String,
                                  kind: String,
                                  className: Option[String],
                                  state: Option[String],
                                  error: Option[String],
                                  metrics: Metrics,
                                  lastModified: Long,
                                  tags: Map[String, JsValue])
      extends Data
  implicit val OBJECT_ABSTRACT_JSON_FORMAT: RootJsonFormat[ObjectAbstract] = jsonFormat9(ObjectAbstract)

  final case class Pipeline(group: String,
                            name: String,
                            flows: Seq[Flow],
                            objects: Set[ObjectAbstract],
                            workerClusterName: Option[String],
                            lastModified: Long,
                            tags: Map[String, JsValue])
      extends Data {

    override def kind: String = "pipeline"
    def rules: Map[String, Set[String]] = flows.map { flow =>
      flow.from.name -> flow.to.map(_.name)
    }.toMap
  }
  implicit val PIPELINE_JSON_FORMAT: RootJsonFormat[Pipeline] = new RootJsonFormat[Pipeline] {
    private[this] val workerClusterNameKey = "workerClusterName"
    private[this] val flowsKey = "flows"
    private[this] val rulesKey = "rules"
    private[this] val objectsKey = "objects"
    private[this] val lastModifiedKey = "lastModified"

    override def read(json: JsValue): Pipeline = Pipeline(
      group = noJsNull(json)(Data.GROUP_KEY).convertTo[String],
      name = noJsNull(json)(Data.NAME_KEY).convertTo[String],
      workerClusterName = noJsNull(json).get(workerClusterNameKey).map(_.convertTo[String]),
      flows = noJsNull(json)
        .get(flowsKey)
        .map(_.asInstanceOf[JsArray].elements.map(FLOW_JSON_FORMAT.read).toSeq)
        .getOrElse(Seq.empty),
      objects = noJsNull(json)(objectsKey).asInstanceOf[JsArray].elements.map(OBJECT_ABSTRACT_JSON_FORMAT.read).toSet,
      lastModified = noJsNull(json)(lastModifiedKey).asInstanceOf[JsNumber].value.toLong,
      tags = noJsNull(json)(Data.TAGS_KEY).asJsObject.fields
    )
    override def write(obj: Pipeline): JsValue = JsObject(
      noJsNull(
        Map(
          Data.GROUP_KEY -> JsString(obj.group),
          Data.NAME_KEY -> JsString(obj.name),
          workerClusterNameKey -> obj.workerClusterName.map(JsString(_)).getOrElse(JsNull),
          flowsKey -> JsArray(obj.flows.map(FLOW_JSON_FORMAT.write).toVector),
          rulesKey -> JsObject(obj.rules.map { e =>
            e._1 -> JsArray(e._2.map(JsString(_)).toVector)
          }),
          objectsKey -> JsArray(obj.objects.map(OBJECT_ABSTRACT_JSON_FORMAT.write).toVector),
          lastModifiedKey -> JsNumber(obj.lastModified),
          Data.TAGS_KEY -> JsObject(obj.tags)
        ))
    )
  }

  /**
    * used to generate the payload and url for POST/PUT request.
    */
  trait Request {

    @Optional("default def is a Data.GROUP_DEFAULT")
    def group(group: String): Request

    @Optional("default name is a random string. But it is required in updating")
    def name(name: String): Request

    @Optional("useless field")
    def workerClusterName(workerClusterName: String): Request

    @Optional("default value is empty")
    def flows(flows: Seq[Flow]): Request

    @Optional("default value is empty")
    def flow(from: ObjectKey, to: ObjectKey): Request = flow(from, Set(to))

    @Optional("default value is empty")
    def flow(from: ObjectKey, to: Set[ObjectKey]): Request = flow(Flow(from = from, to = to))

    @Optional("default value is empty")
    def flow(flow: Flow): Request = flows(Seq(Objects.requireNonNull(flow)))

    @Optional("default value is empty array in creation and None in update")
    def tags(tags: Map[String, JsValue]): Request

    private[v0] def creation: Creation

    private[v0] def update: Update

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

  class Access private[v0] extends com.island.ohara.client.configurator.v0.Access[Pipeline](PIPELINES_PREFIX_PATH) {
    def request: Request = new Request {
      private[this] var group: String = Data.GROUP_DEFAULT
      private[this] var name: String = _
      private[this] var workerClusterName: Option[String] = None
      private[this] var flows: Seq[Flow] = _
      private[this] var tags: Map[String, JsValue] = _

      override def group(group: String): Request = {
        this.group = CommonUtils.requireNonEmpty(group)
        this
      }

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

      override def tags(tags: Map[String, JsValue]): Request = {
        this.tags = Objects.requireNonNull(tags)
        this
      }

      override private[v0] def creation: Creation = Creation(
        group = CommonUtils.requireNonEmpty(group),
        name = if (CommonUtils.isEmpty(name)) CommonUtils.randomString(10) else name,
        workerClusterName = workerClusterName,
        flows = if (flows == null) Seq.empty else flows,
        tags = if (tags == null) Map.empty else tags
      )

      override private[v0] def update: Update = Update(
        workerClusterName = workerClusterName,
        flows = Option(flows),
        tags = Option(tags)
      )

      override def create()(implicit executionContext: ExecutionContext): Future[Pipeline] =
        exec.post[Creation, Pipeline, ErrorApi.Error](
          _url,
          creation
        )
      override def update()(implicit executionContext: ExecutionContext): Future[Pipeline] =
        exec.put[Update, Pipeline, ErrorApi.Error](
          _url(ObjectKey.of(group, name)),
          update
        )
    }
  }

  def access: Access = new Access
}
