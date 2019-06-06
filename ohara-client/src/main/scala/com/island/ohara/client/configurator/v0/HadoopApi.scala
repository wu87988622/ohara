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
import spray.json.RootJsonFormat

import scala.concurrent.{ExecutionContext, Future}

object HadoopApi {
  val HDFS_PREFIX_PATH: String = "hdfs"
  final case class Update(uri: String)
  implicit val HDFS_UPDATE_JSON_FORMAT: RootJsonFormat[Update] = jsonFormat1(Update)
  final case class Creation(name: String, uri: String) extends CreationRequest
  implicit val HDFS_CREATION_JSON_FORMAT: RootJsonFormat[Creation] = jsonFormat2(Creation)

  final case class HdfsInfo(name: String, uri: String, lastModified: Long) extends Data {
    override def id: String = name
    override def kind: String = "hdfs"
  }
  implicit val HDFS_INFO_JSON_FORMAT: RootJsonFormat[HdfsInfo] = jsonFormat3(HdfsInfo)

  /**
    * used to generate the payload and url for POST/PUT request.
    */
  trait Request {
    def name(name: String): Request
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

  class Access private[v0] extends Access2[HdfsInfo](HDFS_PREFIX_PATH) {
    def request(): Request = new Request {
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
        exec.post[Creation, HdfsInfo, ErrorApi.Error](url,
                                                      Creation(
                                                        name = CommonUtils.requireNonEmpty(name),
                                                        uri = CommonUtils.requireNonEmpty(uri)
                                                      ))
      override def update()(implicit executionContext: ExecutionContext): Future[HdfsInfo] =
        exec.put[Update, HdfsInfo, ErrorApi.Error](s"$url/${CommonUtils.requireNonEmpty(name)}",
                                                   Update(
                                                     uri = CommonUtils.requireNonEmpty(uri)
                                                   ))
    }
  }
  def access(): Access = new Access
}
