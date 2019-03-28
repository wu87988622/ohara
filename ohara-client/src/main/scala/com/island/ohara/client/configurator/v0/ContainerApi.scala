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

object ContainerApi {
  final case class PortPair(hostPort: Int, containerPort: Int)
  implicit val PORT_PAIR_JSON_FORMAT: RootJsonFormat[PortPair] = jsonFormat2(PortPair)

  final case class PortMapping(hostIp: String, portPairs: Seq[PortPair])
  implicit val PORT_MAPPING_JSON_FORMAT: RootJsonFormat[PortMapping] = jsonFormat2(PortMapping)

  final case class ContainerInfo(nodeName: String,
                                 id: String,
                                 imageName: String,
                                 created: String,
                                 state: String,
                                 kind: String,
                                 name: String,
                                 size: String,
                                 portMappings: Seq[PortMapping],
                                 environments: Map[String, String],
                                 hostname: String)
  implicit val CONTAINER_INFO_JSON_FORMAT: RootJsonFormat[ContainerInfo] = jsonFormat11(ContainerInfo)
}
