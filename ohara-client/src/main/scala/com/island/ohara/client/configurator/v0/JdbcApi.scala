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
import spray.json.{JsValue, RootJsonFormat}

import scala.concurrent.{ExecutionContext, Future}

object JdbcApi {
  val JDBC_PREFIX_PATH: String = "jdbc"
  final case class Update(url: Option[String],
                          user: Option[String],
                          password: Option[String],
                          tags: Option[Map[String, JsValue]])
  implicit val JDBC_UPDATE_JSON_FORMAT: RootJsonFormat[Update] =
    JsonRefiner[Update].format(jsonFormat4(Update)).rejectEmptyString().refine

  final case class Creation(name: String, url: String, user: String, password: String, tags: Map[String, JsValue])
      extends CreationRequest
  implicit val JDBC_CREATION_JSON_FORMAT: OharaJsonFormat[Creation] =
    JsonRefiner[Creation]
      .format(jsonFormat5(Creation))
      .rejectEmptyString()
      .stringRestriction(Data.NAME_KEY)
      .withNumber()
      .withCharset()
      .withDot()
      .withDash()
      .withUnderLine()
      .toRefiner
      .nullToString("name", () => CommonUtils.randomString(10))
      .nullToEmptyObject(Data.TAGS_KEY)
      .refine

  final case class JdbcInfo(name: String,
                            url: String,
                            user: String,
                            password: String,
                            lastModified: Long,
                            tags: Map[String, JsValue])
      extends Data {
    // TODO: this will be resolved by https://github.com/oharastream/ohara/issues/1734 ... by chia
    override def group: String = Data.GROUP_DEFAULT
    override def kind: String = "jdbc"
  }
  implicit val JDBC_INFO_JSON_FORMAT: RootJsonFormat[JdbcInfo] = jsonFormat6(JdbcInfo)

  trait Request {
    @Optional("default name is a random string. But it is required in updating")
    def name(name: String): Request

    @Optional("it is ignorable if you are going to send update request")
    def url(url: String): Request

    @Optional("it is ignorable if you are going to send update request")
    def user(user: String): Request

    @Optional("it is ignorable if you are going to send update request")
    def password(password: String): Request

    @Optional("default value is empty array")
    def tags(tags: Map[String, JsValue]): Request

    private[v0] def creation: Creation

    private[v0] def update: Update

    /**
      * generate the POST request
      * @param executionContext thread pool
      * @return created data
      */
    def create()(implicit executionContext: ExecutionContext): Future[JdbcInfo]

    /**
      * generate the PUT request
      * @param executionContext thread pool
      * @return updated/created data
      */
    def update()(implicit executionContext: ExecutionContext): Future[JdbcInfo]
  }

  class Access private[v0] extends com.island.ohara.client.configurator.v0.Access[JdbcInfo](JDBC_PREFIX_PATH) {
    def request: Request = new Request {
      private[this] var name: String = _
      private[this] var url: String = _
      private[this] var user: String = _
      private[this] var password: String = _
      private[this] var tags: Map[String, JsValue] = _

      override def name(name: String): Request = {
        this.name = CommonUtils.requireNonEmpty(name)
        this
      }

      override def url(url: String): Request = {
        this.url = CommonUtils.requireNonEmpty(url)
        this
      }

      override def user(user: String): Request = {
        this.user = CommonUtils.requireNonEmpty(user)
        this
      }

      override def password(password: String): Request = {
        this.password = CommonUtils.requireNonEmpty(password)
        this
      }

      override def tags(tags: Map[String, JsValue]): Request = {
        this.tags = Objects.requireNonNull(tags)
        this
      }

      override private[v0] def creation: Creation = Creation(
        name = if (CommonUtils.isEmpty(name)) CommonUtils.randomString(10) else name,
        url = CommonUtils.requireNonEmpty(url),
        user = CommonUtils.requireNonEmpty(user),
        password = CommonUtils.requireNonEmpty(password),
        tags = if (tags == null) Map.empty else tags
      )

      override private[v0] def update: Update = Update(
        url = Option(url).map(CommonUtils.requireNonEmpty),
        user = Option(user).map(CommonUtils.requireNonEmpty),
        password = Option(password).map(CommonUtils.requireNonEmpty),
        tags = Option(tags)
      )

      override def create()(implicit executionContext: ExecutionContext): Future[JdbcInfo] =
        exec.post[Creation, JdbcInfo, ErrorApi.Error](
          _url,
          creation
        )
      override def update()(implicit executionContext: ExecutionContext): Future[JdbcInfo] =
        exec.put[Update, JdbcInfo, ErrorApi.Error](
          s"${_url}/${CommonUtils.requireNonEmpty(name)}",
          update
        )
    }
  }

  def access: Access = new Access
}
