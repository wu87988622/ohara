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
import com.island.ohara.kafka.connector.json.ObjectKey
import spray.json.DefaultJsonProtocol._
import spray.json.{JsValue, RootJsonFormat}

import scala.concurrent.{ExecutionContext, Future}

final object ShabondiApi {
  val GROUP_DEFAULT: String = Data.GROUP_DEFAULT

  /**
    * shabondi does not support group. However, we are in group world and there are many cases of inputting key (group, name)
    * to access resource. This method used to generate key for hostname of node.
    * @param name name
    * @return object key
    */
  def key(name: String): ObjectKey = ObjectKey.of(GROUP_DEFAULT, name)

  val IMAGE_NAME_DEFAULT: String = s"oharastream/shabondi:${VersionUtils.VERSION}"
  val PATH_PREFIX = "shabondi"

  final case class ShabondiDescription(name: String,
                                       lastModified: Long,
                                       state: Option[String] = None,
                                       to: Seq[String] = Seq.empty,
                                       port: Int,
                                       instances: Int)
      extends Data {

    // Shabondi does not support to define group
    override def group: String = GROUP_DEFAULT
    override def kind: String = "shabondi"

    // TODO: Does shabondi need the tags? by chia
    override def tags: Map[String, JsValue] = Map.empty
  }

  final case class ShabondiProperty(to: Option[Seq[String]], port: Option[Int])

  implicit val SHABONDI_DESCRIPTION_JSON_FORMAT: RootJsonFormat[ShabondiDescription] = jsonFormat6(ShabondiDescription)
  implicit val SHABONDI_PROPERTY_JSON_FORMAT: RootJsonFormat[ShabondiProperty] = jsonFormat2(ShabondiProperty)

  def access: ShabondiAccess = new ShabondiAccess()

  class ShabondiAccess extends BasicAccess(PATH_PREFIX) {

    private def basicUrl(name: String = "") = {
      val url = s"http://${_hostname}:${_port}/${_version}/${_prefixPath}"
      if (name.isEmpty) url else url + "/" + name
    }

    def add()(implicit executionContext: ExecutionContext): Future[ShabondiDescription] = {
      val url = basicUrl()
      exec.post[ShabondiDescription, ErrorApi.Error](url)
    }

    def getProperty(name: String)(implicit executionContext: ExecutionContext): Future[ShabondiDescription] = {
      val url = basicUrl(name)
      exec.get[ShabondiDescription, ErrorApi.Error](url)
    }

    def updateProperty(name: String, property: ShabondiProperty)(
      implicit executionContext: ExecutionContext): Future[ShabondiDescription] = {
      val url = basicUrl(name)
      exec.put[ShabondiProperty, ShabondiDescription, ErrorApi.Error](url, property)
    }

    def delete(name: String)(implicit executionContext: ExecutionContext): Future[Unit] =
      exec.delete[ErrorApi.Error](basicUrl(name))

    def start(name: String)(implicit executionContext: ExecutionContext): Future[ShabondiDescription] = {
      val url = basicUrl(name) + "/" + START_COMMAND
      exec.put[ShabondiDescription, ErrorApi.Error](url)
    }

    def stop(name: String)(implicit executionContext: ExecutionContext): Future[ShabondiDescription] = {
      val url = basicUrl(name) + "/" + STOP_COMMAND
      exec.put[ShabondiDescription, ErrorApi.Error](url)
    }
  }

}
