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

package com.island.ohara.configurator.route

import akka.http.scaladsl.server
import com.island.ohara.client.configurator.v0.FtpInfoApi._
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.route.hook.{HookOfCreation, HookOfUpdating}
import com.island.ohara.configurator.store.DataStore
import spray.json.DeserializationException

import scala.concurrent.{ExecutionContext, Future}

private[configurator] object FtpInfoRoute {

  private[this] def creationToFtpInfo(creation: Creation): Future[FtpInfo] = Future.successful(
    FtpInfo(
      group = creation.group,
      name = creation.name,
      hostname = creation.hostname,
      port = creation.port,
      user = creation.user,
      password = creation.password,
      lastModified = CommonUtils.current(),
      tags = creation.tags
    ))

  private[this] def hookOfCreation: HookOfCreation[Creation, FtpInfo] = creationToFtpInfo(_)

  private[this] def hookOfUpdating: HookOfUpdating[Updating, FtpInfo] =
    (key: ObjectKey, updating: Updating, previousOption: Option[FtpInfo]) =>
      previousOption match {
        case None =>
          creationToFtpInfo(
            Creation(
              group = key.group,
              name = key.name,
              hostname = updating.hostname.getOrElse(
                throw DeserializationException(s"hostname is required", fieldNames = List("hostname"))),
              port =
                updating.port.getOrElse(throw DeserializationException(s"port is required", fieldNames = List("port"))),
              user =
                updating.user.getOrElse(throw DeserializationException(s"user is required", fieldNames = List("user"))),
              password = updating.password.getOrElse(
                throw DeserializationException(s"user is required", fieldNames = List("user"))),
              tags = updating.tags.getOrElse(Map.empty)
            ))
        case Some(previous) =>
          creationToFtpInfo(
            Creation(
              group = key.group,
              name = key.name,
              hostname = updating.hostname.getOrElse(previous.hostname),
              port = updating.port.getOrElse(previous.port),
              user = updating.user.getOrElse(previous.user),
              password = updating.password.getOrElse(previous.password),
              tags = updating.tags.getOrElse(previous.tags)
            ))
    }

  def apply(implicit store: DataStore, executionContext: ExecutionContext): server.Route =
    RouteBuilder[Creation, Updating, FtpInfo]()
      .root(FTP_PREFIX_PATH)
      .hookOfCreation(hookOfCreation)
      .hookOfUpdating(hookOfUpdating)
      .build()
}
