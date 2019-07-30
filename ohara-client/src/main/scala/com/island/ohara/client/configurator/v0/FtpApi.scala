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

import com.island.ohara.common.annotations.{Optional, VisibleForTesting}
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.kafka.connector.json.ObjectKey
import spray.json.DefaultJsonProtocol._
import spray.json.{JsValue, RootJsonFormat}

import scala.concurrent.{ExecutionContext, Future}
object FtpApi {
  val FTP_PREFIX_PATH: String = "ftp"
  final case class Update(hostname: Option[String],
                          port: Option[Int],
                          user: Option[String],
                          password: Option[String],
                          tags: Option[Map[String, JsValue]])
  implicit val FTP_UPDATE_JSON_FORMAT: RootJsonFormat[Update] =
    JsonRefiner[Update].format(jsonFormat5(Update)).requireConnectionPort("port").rejectEmptyString().refine

  final case class Creation(name: String,
                            hostname: String,
                            port: Int,
                            user: String,
                            password: String,
                            tags: Map[String, JsValue])
      extends CreationRequest
  implicit val FTP_CREATION_JSON_FORMAT: OharaJsonFormat[Creation] =
    JsonRefiner[Creation]
      .format(jsonFormat6(Creation))
      .requireConnectionPort("port")
      .rejectEmptyString()
      .stringRestriction(Set(Data.GROUP_KEY, Data.NAME_KEY))
      .withNumber()
      .withCharset()
      .withDot()
      .withDash()
      .withUnderLine()
      .toRefiner
      .nullToString(Data.NAME_KEY, () => CommonUtils.randomString(10))
      .nullToEmptyObject(Data.TAGS_KEY)
      .refine

  final case class FtpInfo(group: String,
                           name: String,
                           hostname: String,
                           port: Int,
                           user: String,
                           password: String,
                           lastModified: Long,
                           tags: Map[String, JsValue])
      extends Data {
    override def kind: String = "ftp"
  }
  implicit val FTP_INFO_JSON_FORMAT: RootJsonFormat[FtpInfo] = jsonFormat8(FtpInfo)

  /**
    * used to generate the payload and url for POST/PUT request.
    */
  trait Request {
    @Optional("default def is a Data.GROUP_DEFAULT")
    def group(group: String): Request

    @Optional("default name is a random string. But it is required in updating")
    def name(name: String): Request

    @Optional("it is ignorable if you are going to send update request")
    def hostname(hostname: String): Request

    @Optional("it is ignorable if you are going to send update request")
    def port(port: Int): Request

    @Optional("it is ignorable if you are going to send update request")
    def user(user: String): Request

    @Optional("it is ignorable if you are going to send update request")
    def password(password: String): Request

    @Optional("default value is empty array in creation and None in update")
    def tags(tags: Map[String, JsValue]): Request

    /**
      * Retrieve the inner object of request payload. Noted, it throw unchecked exception if you haven't filled all required fields
      * @return the payload of creation
      */
    @VisibleForTesting
    private[v0] def creation: Creation

    /**
      * Retrieve the inner object of request payload. Noted, it throw unchecked exception if you haven't filled all required fields
      * @return the payload of update
      */
    @VisibleForTesting
    private[v0] def update: Update

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

  class Access private[v0] extends com.island.ohara.client.configurator.v0.Access[FtpInfo](FTP_PREFIX_PATH) {
    def request: Request = new Request {
      private[this] var group: String = Data.GROUP_DEFAULT
      private[this] var name: String = _
      private[this] var port: Option[Int] = None
      private[this] var hostname: String = _
      private[this] var user: String = _
      private[this] var password: String = _
      private[this] var tags: Map[String, JsValue] = _

      override def group(group: String): Request = {
        this.group = CommonUtils.requireNonEmpty(group)
        this
      }

      override def name(name: String): Request = {
        this.name = CommonUtils.requireNonEmpty(name)
        this
      }

      override def port(port: Int): Request = {
        this.port = Some(CommonUtils.requireConnectionPort(port))
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

      override def tags(tags: Map[String, JsValue]): Request = {
        this.tags = Objects.requireNonNull(tags)
        this
      }

      override private[v0] def creation: Creation = Creation(
        name = if (CommonUtils.isEmpty(name)) CommonUtils.randomString(10) else name,
        hostname = CommonUtils.requireNonEmpty(hostname),
        port = port.map(CommonUtils.requireConnectionPort).getOrElse(throw new NullPointerException),
        user = CommonUtils.requireNonEmpty(user),
        password = CommonUtils.requireNonEmpty(password),
        tags = if (tags == null) Map.empty else tags
      )

      override private[v0] def update: Update = Update(
        hostname = Option(hostname).map(CommonUtils.requireNonEmpty),
        port = port.map(CommonUtils.requireConnectionPort),
        user = Option(user).map(CommonUtils.requireNonEmpty),
        password = Option(password).map(CommonUtils.requireNonEmpty),
        tags = Option(tags)
      )

      override def create()(implicit executionContext: ExecutionContext): Future[FtpInfo] =
        exec.post[Creation, FtpInfo, ErrorApi.Error](
          urlWithGroup(group),
          creation
        )
      override def update()(implicit executionContext: ExecutionContext): Future[FtpInfo] =
        exec.put[Update, FtpInfo, ErrorApi.Error](
          _url(ObjectKey.of(group, name)),
          update
        )
    }
  }

  def access: Access = new Access
}
