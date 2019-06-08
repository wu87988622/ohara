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
import com.island.ohara.common.annotations.Optional
import com.island.ohara.common.util.CommonUtils
import spray.json.DefaultJsonProtocol._
import spray.json.{JsObject, JsString, JsValue, RootJsonFormat}

import scala.concurrent.{ExecutionContext, Future}

object FtpApi {
  val FTP_PREFIX_PATH: String = "ftp"
  final case class Update(hostname: Option[String], port: Option[Int], user: Option[String], password: Option[String])
  implicit val FTP_UPDATE_JSON_FORMAT: RootJsonFormat[Update] = jsonFormat4(Update)
  final case class Creation(name: String, hostname: String, port: Int, user: String, password: String)
      extends CreationRequest
  implicit val FTP_CREATION_JSON_FORMAT: RootJsonFormat[Creation] = jsonFormat5(Creation)

  final case class FtpInfo(name: String,
                           hostname: String,
                           port: Int,
                           user: String,
                           password: String,
                           lastModified: Long)
      extends Data {
    override def id: String = name
    override def kind: String = "ftp"
  }
  implicit val FTP_INFO_JSON_FORMAT: RootJsonFormat[FtpInfo] = new RootJsonFormat[FtpInfo] {
    private[this] val format = jsonFormat6(FtpInfo)
    override def read(json: JsValue): FtpInfo = format.read(json)
    override def write(obj: FtpInfo): JsValue = JsObject(
      // TODO: remove the id
      format.write(obj).asJsObject.fields ++ Map("id" -> JsString(obj.id)))
  }

  /**
    * used to generate the payload and url for POST/PUT request.
    */
  trait Request {
    def name(name: String): Request
    @Optional("it is ignorable if you are going to send update request")
    def hostname(hostname: String): Request
    @Optional("it is ignorable if you are going to send update request")
    def port(port: Int): Request
    @Optional("it is ignorable if you are going to send update request")
    def user(user: String): Request
    @Optional("it is ignorable if you are going to send update request")
    def password(password: String): Request

    /**
      * generate the POST request
      * @param executionContext thread pool
      * @return created data
      */
    def create()(implicit executionContext: ExecutionContext): Future[FtpInfo]

    /**
      * generate the PUT request
      * @param executionContext thread pool
      * @return updated/created data
      */
    def update()(implicit executionContext: ExecutionContext): Future[FtpInfo]
  }

  class Access private[v0] extends Access2[FtpInfo](FTP_PREFIX_PATH) {
    def request(): Request = new Request {
      private[this] var name: String = _
      private[this] var port: Option[Int] = None
      private[this] var hostname: String = _
      private[this] var user: String = _
      private[this] var password: String = _

      override def name(name: String): Request = {
        this.name = CommonUtils.requireNonEmpty(name)
        this
      }

      override def port(port: Int): Request = {
        this.port = Some(CommonUtils.requirePositiveInt(port))
        this
      }

      override def hostname(hostname: String): Request = {
        this.hostname = CommonUtils.requireNonEmpty(hostname)
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

      override def create()(implicit executionContext: ExecutionContext): Future[FtpInfo] =
        exec.post[Creation, FtpInfo, ErrorApi.Error](
          _url,
          Creation(
            name = CommonUtils.requireNonEmpty(name),
            hostname = CommonUtils.requireNonEmpty(hostname),
            port = port.map(CommonUtils.requirePositiveInt).getOrElse(throw new NullPointerException),
            user = CommonUtils.requireNonEmpty(user),
            password = CommonUtils.requireNonEmpty(password)
          )
        )
      override def update()(implicit executionContext: ExecutionContext): Future[FtpInfo] =
        exec.put[Update, FtpInfo, ErrorApi.Error](
          s"${_url}/${CommonUtils.requireNonEmpty(name)}",
          Update(
            hostname = Option(hostname).map(CommonUtils.requireNonEmpty),
            port = port.map(CommonUtils.requirePositiveInt),
            user = Option(user).map(CommonUtils.requireNonEmpty),
            password = Option(password).map(CommonUtils.requireNonEmpty),
          )
        )
    }
  }

  def access(): Access = new Access
}
