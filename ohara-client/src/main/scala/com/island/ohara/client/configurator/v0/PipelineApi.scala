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

import com.island.ohara.client.configurator.{Data, QueryRequest}
import com.island.ohara.common.annotations.Optional
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.common.util.CommonUtils
import spray.json.DefaultJsonProtocol._
import spray.json.{JsValue, RootJsonFormat}

import scala.concurrent.{ExecutionContext, Future}

object PipelineApi {
  val KIND: String                  = "pipeline"
  val PIPELINES_PREFIX_PATH: String = "pipelines"

  /**
    * action key. it is used to auto-remove the existent objs from flows,
    */
  val REFRESH_COMMAND: String = "refresh"

  final case class Flow(from: ObjectKey, to: Set[ObjectKey])
  implicit val FLOW_JSON_FORMAT: OharaJsonFormat[Flow] =
    JsonRefiner[Flow].format(jsonFormat2(Flow)).rejectEmptyString().refine

  final case class Endpoint(group: String, name: String, kind: Option[String]) {
    def key: ObjectKey = ObjectKey.of(group, name)
  }
  implicit val ENDPOINT_JSON_FORMAT: OharaJsonFormat[Endpoint] =
    JsonRefiner[Endpoint]
      .format(jsonFormat3(Endpoint))
      .nullToString(GROUP_KEY, GROUP_DEFAULT)
      .rejectEmptyString()
      .refine

  /**
    * @param flows  this filed is declared as option type since ohara supports partial update. Empty array means you want to **cleanup** this
    *               field. And none means you don't want to change any bit of this field.
    */
  final case class Updating(
    // TODO: remove this helper (https://github.com/oharastream/ohara/issues/3530)
    flows: Option[Seq[Flow]],
    endpoints: Option[Set[Endpoint]],
    tags: Option[Map[String, JsValue]]
  )

  implicit val UPDATING_JSON_FORMAT: RootJsonFormat[Updating] =
    JsonRefiner[Updating].format(jsonFormat3(Updating)).rejectEmptyString().refine

  final case class Creation(
    group: String,
    name: String,
    // TODO: remove flows (https://github.com/oharastream/ohara/issues/3530)
    flows: Seq[Flow],
    endpoints: Set[Endpoint],
    tags: Map[String, JsValue]
  ) extends com.island.ohara.client.configurator.v0.BasicCreation

  implicit val CREATION_JSON_FORMAT: OharaJsonFormat[Creation] =
    // this object is open to user define the (group, name) in UI, we need to handle the key rules
    rulesOfKey[Creation]
      .format(jsonFormat5(Creation))
      .rejectEmptyString()
      .nullToEmptyObject(TAGS_KEY)
      .nullToEmptyArray("flows")
      .nullToEmptyArray("endpoints")
      .refine

  import MetricsApi._

  final case class ObjectAbstract(
    group: String,
    name: String,
    kind: String,
    className: Option[String],
    state: Option[String],
    error: Option[String],
    metrics: Metrics,
    lastModified: Long,
    tags: Map[String, JsValue]
  ) extends Data
  implicit val OBJECT_ABSTRACT_JSON_FORMAT: RootJsonFormat[ObjectAbstract] = jsonFormat9(ObjectAbstract)

  final case class Pipeline(
    group: String,
    name: String,
    flows: Seq[Flow],
    endpoints: Set[Endpoint],
    objects: Set[ObjectAbstract],
    jarKeys: Set[ObjectKey],
    lastModified: Long,
    tags: Map[String, JsValue]
  ) extends Data {
    override def kind: String = KIND
    // TODO: remove this helper (https://github.com/oharastream/ohara/issues/3530)
    def _endpoints: Set[Endpoint] =
      if (endpoints.isEmpty) flows.flatMap(f => f.to + f.from).map(o => Endpoint(o.group, o.name, None)).toSet
      else endpoints
  }

  implicit val PIPELINE_JSON_FORMAT: RootJsonFormat[Pipeline] = jsonFormat8(Pipeline)

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

    // TODO: remove this helper (https://github.com/oharastream/ohara/issues/3530)
    @Optional("default value is empty")
    def flow(from: ObjectKey, to: ObjectKey): Request = flow(from, Set(to))

    // TODO: remove this helper (https://github.com/oharastream/ohara/issues/3530)
    @Optional("default value is empty")
    def flow(from: ObjectKey, to: Set[ObjectKey]): Request = flow(Flow(from = from, to = to))

    // TODO: remove this helper (https://github.com/oharastream/ohara/issues/3530)
    @Optional("default value is empty")
    def flow(flow: Flow): Request = flows(Seq(Objects.requireNonNull(flow)))

    // TODO: remove this helper (https://github.com/oharastream/ohara/issues/3530)
    @Optional("default value is empty")
    def flows(flows: Seq[Flow]): Request

    @Optional("default value is empty")
    def endpoint(data: Data): Request = endpoint(Endpoint(group = data.group, name = data.name, kind = Some(data.kind)))

    @Optional("default value is empty")
    def endpoint(endpoint: Endpoint): Request = endpoints(Set(Objects.requireNonNull(endpoint)))

    @Optional("default value is empty")
    def endpoints(endpoints: Set[Endpoint]): Request

    @Optional("default value is empty array in creation and None in update")
    def tags(tags: Map[String, JsValue]): Request

    private[v0] def creation: Creation

    private[v0] def updating: Updating

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

  sealed trait Query extends BasicQuery[Pipeline] {
    // TODO: there are a lot of settings which is worth of having parameters ... by chia
  }

  class Access private[v0]
      extends com.island.ohara.client.configurator.v0.Access[Creation, Updating, Pipeline](PIPELINES_PREFIX_PATH) {
    def refresh(key: ObjectKey)(implicit executionContext: ExecutionContext): Future[Unit] = put(key, REFRESH_COMMAND)

    def query: Query = new Query {
      override protected def doExecute(request: QueryRequest)(
        implicit executionContext: ExecutionContext
      ): Future[Seq[Pipeline]] = list(request)
    }

    def request: Request = new Request {
      private[this] var group: String = GROUP_DEFAULT
      private[this] var name: String  = _
      // TODO: remove this helper (https://github.com/oharastream/ohara/issues/3530)
      private[this] var flows: Seq[Flow]           = _
      private[this] var endpoints: Set[Endpoint]   = _
      private[this] var tags: Map[String, JsValue] = _

      override def group(group: String): Request = {
        this.group = CommonUtils.requireNonEmpty(group)
        this
      }

      override def name(name: String): Request = {
        this.name = CommonUtils.requireNonEmpty(name)
        this
      }

      // TODO: remove this helper (https://github.com/oharastream/ohara/issues/3530)
      override def flows(flows: Seq[Flow]): Request = {
        if (this.flows == null) this.flows = Objects.requireNonNull(flows)
        else this.flows ++= Objects.requireNonNull(flows)
        this
      }

      override def endpoints(endpoints: Set[Endpoint]): Request = {
        if (this.endpoints == null) this.endpoints = Objects.requireNonNull(endpoints)
        else this.endpoints ++= Objects.requireNonNull(endpoints)
        this
      }

      override def tags(tags: Map[String, JsValue]): Request = {
        if (this.tags == null) this.tags = Objects.requireNonNull(tags)
        else this.tags ++= Objects.requireNonNull(tags)
        this
      }

      override private[v0] def creation: Creation =
        // auto-complete the creation via our refiner
        CREATION_JSON_FORMAT.read(
          CREATION_JSON_FORMAT.write(
            Creation(
              group = CommonUtils.requireNonEmpty(group),
              name = if (CommonUtils.isEmpty(name)) CommonUtils.randomString(10) else name,
              flows = if (flows == null) Seq.empty else flows,
              endpoints = if (endpoints == null) Set.empty else endpoints,
              tags = if (tags == null) Map.empty else tags
            )
          )
        )

      override private[v0] def updating: Updating =
        // auto-complete the updating via our refiner
        UPDATING_JSON_FORMAT.read(
          UPDATING_JSON_FORMAT.write(
            Updating(
              flows = Option(flows),
              endpoints = Option(endpoints),
              tags = Option(tags)
            )
          )
        )

      override def create()(implicit executionContext: ExecutionContext): Future[Pipeline] = post(creation)
      override def update()(implicit executionContext: ExecutionContext): Future[Pipeline] =
        put(ObjectKey.of(group, name), updating)
    }
  }

  def access: Access = new Access
}
