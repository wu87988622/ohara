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

import com.island.ohara.common.util.VersionUtils
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.{ExecutionContext, Future}

final object ShabondiApi {
  val IMAGE_NAME_DEFAULT: String = s"oharastream/shabondi:${VersionUtils.VERSION}"
  val PATH_PREFIX = "shabondi"
  val PATH_SEGMENT_START = "start"
  val PATH_SEGMENT_STOP = "stop"

  final case class ShabondiDescription(id: String,
                                       name: String,
                                       lastModified: Long,
                                       state: Option[String] = None,
                                       to: Seq[String] = Seq.empty,
                                       port: Int,
                                       instances: Int)
      extends Data {
    override def kind: String = "shabondi"
  }

  final case class ShabondiProperty(name: Option[String], to: Option[Seq[String]], port: Option[Int])

  implicit val SHABONDI_DESCRIPTION_JSON_FORMAT: RootJsonFormat[ShabondiDescription] = jsonFormat7(ShabondiDescription)
  implicit val SHABONDI_PROPERTY_JSON_FORMAT: RootJsonFormat[ShabondiProperty] = jsonFormat3(ShabondiProperty)

  def access(): ShabondiAccess = new ShabondiAccess()

  class ShabondiAccess extends BasicAccess(PATH_PREFIX) {

    private def basicUrl(id: String = "") = {
      val url = s"http://${_hostname}:${_port}/${_version}/${_prefixPath}"
      if (id.isEmpty) url else url + "/" + id
    }

    def add()(implicit executionContext: ExecutionContext): Future[ShabondiDescription] = {
      val url = basicUrl()
      exec.post[ShabondiDescription, ErrorApi.Error](url)
    }

    def getProperty(id: String)(implicit executionContext: ExecutionContext): Future[ShabondiDescription] = {
      val url = basicUrl(id)
      exec.get[ShabondiDescription, ErrorApi.Error](url)
    }

    def updateProperty(id: String, property: ShabondiProperty)(
      implicit executionContext: ExecutionContext): Future[ShabondiDescription] = {
      val url = basicUrl(id)
      exec.put[ShabondiProperty, ShabondiDescription, ErrorApi.Error](url, property)
    }

    def delete(id: String)(implicit executionContext: ExecutionContext): Future[ShabondiDescription] = {
      val url = basicUrl(id)
      exec.delete[ShabondiDescription, ErrorApi.Error](url)
    }

    def start(id: String)(implicit executionContext: ExecutionContext): Future[ShabondiDescription] = {
      val url = basicUrl(id) + "/" + PATH_SEGMENT_START
      exec.put[ShabondiDescription, ErrorApi.Error](url)
    }

    def stop(id: String)(implicit executionContext: ExecutionContext): Future[ShabondiDescription] = {
      val url = basicUrl(id) + "/" + PATH_SEGMENT_STOP
      exec.put[ShabondiDescription, ErrorApi.Error](url)
    }
  }

}
