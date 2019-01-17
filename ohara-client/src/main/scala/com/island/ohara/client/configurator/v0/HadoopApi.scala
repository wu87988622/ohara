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
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

object HadoopApi {
  val HDFS_PREFIX_PATH: String = "hdfs"
  final case class HdfsInfoRequest(name: String, uri: String)
  implicit val HDFS_INFO_REQUEST_JSON_FORMAT: RootJsonFormat[HdfsInfoRequest] = jsonFormat2(HdfsInfoRequest)

  final case class HdfsInfo(id: String, name: String, uri: String, lastModified: Long) extends Data {
    override def kind: String = "hdfs"
  }
  implicit val HDFS_INFO_JSON_FORMAT: RootJsonFormat[HdfsInfo] = jsonFormat4(HdfsInfo)

  def access(): Access[HdfsInfoRequest, HdfsInfo] =
    new Access[HdfsInfoRequest, HdfsInfo](HDFS_PREFIX_PATH)
}
