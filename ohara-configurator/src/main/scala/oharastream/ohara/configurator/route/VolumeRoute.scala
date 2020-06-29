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

package oharastream.ohara.configurator.route

import akka.http.scaladsl.server
import oharastream.ohara.client.configurator.VolumeApi
import oharastream.ohara.client.configurator.VolumeApi.{Creation, Updating, Volume}
import oharastream.ohara.common.util.CommonUtils
import oharastream.ohara.configurator.store.DataStore
import spray.json.DeserializationException

import scala.concurrent.{ExecutionContext, Future}

private[configurator] object VolumeRoute {
  private[this] def toVolume(creation: Creation): Future[Volume] =
    Future.successful(
      Volume(
        group = creation.group,
        name = creation.name,
        nodeNames = creation.nodeNames,
        path = creation.path,
        state = None,
        error = None,
        tags = creation.tags,
        lastModified = CommonUtils.current()
      )
    )

  def apply(implicit store: DataStore, executionContext: ExecutionContext): server.Route =
    RouteBuilder[Creation, Updating, Volume]()
      .prefixOfPlural("volumes")
      .prefixOfSingular(VolumeApi.KIND)
      .hookOfCreation(toVolume)
      .hookOfUpdating(
        (key, updating, previousOption) =>
          toVolume(previousOption match {
            case None =>
              if (updating.nodeNames.isEmpty)
                throw DeserializationException("nodeNames is required", fieldNames = List("nodeNames"))
              if (updating.path.isEmpty) throw DeserializationException("path is required", fieldNames = List("path"))
              Creation(
                group = key.group(),
                name = key.name(),
                nodeNames = updating.nodeNames.get,
                path = updating.path.get,
                tags = updating.tags.getOrElse(Map.empty)
              )
            case Some(previous) =>
              Creation(
                group = key.group(),
                name = key.name(),
                nodeNames = updating.nodeNames.getOrElse(previous.nodeNames),
                path = updating.path.getOrElse(previous.path),
                tags = updating.tags.getOrElse(previous.tags)
              )
          })
      )
      .hookOfGet(Future.successful(_))
      .hookOfList(Future.successful(_))
      .hookBeforeDelete(_ => Future.unit)
      .build()
}
