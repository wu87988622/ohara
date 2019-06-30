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

object HadoopApi {
  val HDFS_PREFIX_PATH: String = "hdfs"
  final case class Update(uri: Option[String])

  implicit val HDFS_UPDATE_JSON_FORMAT: RootJsonFormat[Update] =
    JsonRefiner[Update].format(jsonFormat1(Update)).rejectEmptyString().refine

  final case class Creation(name: String, uri: String) extends CreationRequest
  implicit val HDFS_CREATION_JSON_FORMAT: OharaJsonFormat[Creation] =
    JsonRefiner[Creation]
      .format(jsonFormat2(Creation))
      .rejectEmptyString()
      .stringRestriction("name")
      .withNumber()
      .withCharset()
      .withDot()
      .withDash()
      .withUnderLine()
      .toRefiner
      .refine

  final case class HdfsInfo(name: String, uri: String, lastModified: Long) extends Data {
    override def id: String = name
    override def kind: String = "hdfs"
  }

  implicit val HDFS_INFO_JSON_FORMAT: RootJsonFormat[HdfsInfo] = new RootJsonFormat[HdfsInfo] {
    private[this] val format = jsonFormat3(HdfsInfo)
    override def read(json: JsValue): HdfsInfo = format.read(json)
    override def write(obj: HdfsInfo): JsValue = JsObject(
      // TODO: remove the id
      format.write(obj).asJsObject.fields ++ Map("id" -> JsString(obj.id)))
  }

  /**
    * used to generate the payload and url for POST/PUT request.
    */
  trait Request {
    def name(name: String): Request
    @Optional("it is ignorable if you are going to send update request")
    def uri(uri: String): Request

    /**
      * generate the POST request
      * @param executionContext thread pool
      * @return created data
      */
    def create()(implicit executionContext: ExecutionContext): Future[HdfsInfo]

    /**
      * generate the PUT request
      * @param executionContext thread pool
      * @return updated/created data
      */
    def update()(implicit executionContext: ExecutionContext): Future[HdfsInfo]
  }

  class Access private[v0] extends com.island.ohara.client.configurator.v0.Access[HdfsInfo](HDFS_PREFIX_PATH) {
    def request: Request = new Request {
      private[this] var name: String = _
      private[this] var uri: String = _
      override def name(name: String): Request = {
        this.name = CommonUtils.requireNonEmpty(name)
        this
      }
      override def uri(uri: String): Request = {
        this.uri = CommonUtils.requireNonEmpty(uri)
        this
      }
      override def create()(implicit executionContext: ExecutionContext): Future[HdfsInfo] =
        exec.post[Creation, HdfsInfo, ErrorApi.Error](_url,
                                                      Creation(
                                                        name = CommonUtils.requireNonEmpty(name),
                                                        uri = CommonUtils.requireNonEmpty(uri)
                                                      ))
      override def update()(implicit executionContext: ExecutionContext): Future[HdfsInfo] =
        exec.put[Update, HdfsInfo, ErrorApi.Error](s"${_url}/${CommonUtils.requireNonEmpty(name)}",
                                                   Update(
                                                     uri = Option(uri).map(CommonUtils.requireNonEmpty)
                                                   ))
    }
  }
  def access: Access = new Access
}
