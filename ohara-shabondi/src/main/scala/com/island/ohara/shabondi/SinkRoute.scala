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

package com.island.ohara.shabondi

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives._
import akka.http.scaladsl.server.directives.PathDirectives._
import akka.http.scaladsl.server.directives.RouteDirectives._

private trait SinkRoute {

  implicit def actorSystem: ActorSystem

  lazy private val log = Logging(actorSystem, classOf[SinkRoute])

  lazy val sinkRoute: Route =
    path("v0") {
      post {
        log.info("[sink route]")
        complete((StatusCodes.OK, "sinkRoute OK"))
      }
    }

}
