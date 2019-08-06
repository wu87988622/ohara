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

import com.island.ohara.client.configurator.Data
import com.island.ohara.common.annotations.Optional
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.kafka.connector.json.ObjectKey
import spray.json.DefaultJsonProtocol._
import spray.json.{JsValue, RootJsonFormat}

import scala.concurrent.{ExecutionContext, Future}

object PipelineApi {

  /**
    * The default value of group for this API.
    */
  val GROUP_DEFAULT: String = com.island.ohara.client.configurator.v0.GROUP_DEFAULT
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

  implicit val PIPELINE_UPDATE_JSON_FORMAT: RootJsonFormat[Update] =
    JsonRefiner[Update].format(jsonFormat3(Update)).rejectEmptyString().refine

  final case class Creation(group: String,
                            name: String,
                            workerClusterName: Option[String],
                            flows: Seq[Flow],
                            tags: Map[String, JsValue])
      extends CreationRequest

  implicit val PIPELINE_CREATION_JSON_FORMAT: OharaJsonFormat[Creation] = JsonRefiner[Creation]
    .format(jsonFormat5(Creation))
    .rejectEmptyString()
    .stringRestriction(Set(GROUP_KEY, NAME_KEY))
    .withNumber()
    .withCharset()
    .withDot()
    .withDash()
    .withUnderLine()
    .toRefiner
    .nullToString(GROUP_KEY, () => GROUP_DEFAULT)
    .nullToString(NAME_KEY, () => CommonUtils.randomString(10))
    .nullToEmptyObject(TAGS_KEY)
    .nullToEmptyArray("flows")
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
  }

  implicit val PIPELINE_JSON_FORMAT: RootJsonFormat[Pipeline] = jsonFormat7(Pipeline)

  /**
    * used to generate the payload and url for POST/PUT request.
    */
  trait Request {

    /**
      * set the group and name via key
      * @param objectKey object key
      * @return this request
      */
    def key(objectKey: ObjectKey): Request = {
      group(objectKey.group())
      name(objectKey.name())
    }

    @Optional("default def is a GROUP_DEFAULT")
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
      private[this] var group: String = GROUP_DEFAULT
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
          url,
          creation
        )
      override def update()(implicit executionContext: ExecutionContext): Future[Pipeline] =
        exec.put[Update, Pipeline, ErrorApi.Error](
          url(ObjectKey.of(group, name)),
          update
        )
    }
  }

  def access: Access = new Access
}
