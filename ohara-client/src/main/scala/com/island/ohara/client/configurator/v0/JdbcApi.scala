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
import spray.json.{JsObject, JsString, JsValue, RootJsonFormat}

import scala.concurrent.{ExecutionContext, Future}

object JdbcApi {
  val JDBC_PREFIX_PATH: String = "jdbc"
  final case class Update(url: Option[String], user: Option[String], password: Option[String])
  implicit val JDBC_UPDATE_JSON_FORMAT: RootJsonFormat[Update] =
    JsonRefiner[Update].format(jsonFormat3(Update)).rejectEmptyString().refine

  final case class Creation(name: String, url: String, user: String, password: String) extends CreationRequest
  implicit val JDBC_CREATION_JSON_FORMAT: OharaJsonFormat[Creation] =
    JsonRefiner[Creation]
      .format(jsonFormat4(Creation))
      .rejectEmptyString()
      .stringRestriction("name")
      .withNumber()
      .withCharset()
      .withDot()
      .withDash()
      .withUnderLine()
      .toRefiner
      .nullToString("name", () => CommonUtils.randomString(10))
      .refine

  final case class JdbcInfo(name: String, url: String, user: String, password: String, lastModified: Long)
      extends Data {
    override def id: String = name
    override def kind: String = "jdbc"
  }
  implicit val JDBC_INFO_JSON_FORMAT: RootJsonFormat[JdbcInfo] = new RootJsonFormat[JdbcInfo] {
    private[this] val format = jsonFormat5(JdbcInfo)
    override def read(json: JsValue): JdbcInfo = format.read(json)
    override def write(obj: JdbcInfo): JsValue = JsObject(
      // TODO: remove the id
      format.write(obj).asJsObject.fields ++ Map("id" -> JsString(obj.id)))
  }

  trait Request {
    def name(name: String): Request
    def url(url: String): Request
    def user(user: String): Request
    def password(password: String): Request

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

      override def create()(implicit executionContext: ExecutionContext): Future[JdbcInfo] =
        exec.post[Creation, JdbcInfo, ErrorApi.Error](
          _url,
          Creation(
            name = CommonUtils.requireNonEmpty(name),
            url = CommonUtils.requireNonEmpty(url),
            user = CommonUtils.requireNonEmpty(user),
            password = CommonUtils.requireNonEmpty(password)
          )
        )
      override def update()(implicit executionContext: ExecutionContext): Future[JdbcInfo] =
        exec.put[Update, JdbcInfo, ErrorApi.Error](
          s"${_url}/${CommonUtils.requireNonEmpty(name)}",
          Update(
            url = Option(url).map(CommonUtils.requireNonEmpty),
            user = Option(user).map(CommonUtils.requireNonEmpty),
            password = Option(password).map(CommonUtils.requireNonEmpty),
          )
        )
    }
  }

  def access: Access = new Access
}
