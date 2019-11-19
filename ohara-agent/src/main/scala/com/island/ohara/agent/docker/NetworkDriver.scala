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

package com.island.ohara.agent.docker
import com.island.ohara.client.Enum

/**
  * docker's network driver.
  */
sealed abstract class NetworkDriver

object NetworkDriver extends Enum[NetworkDriver] {
  /**
    * The default network driver.
    * If you don’t specify a driver, this is the type of network you are creating.
    * Bridge networks are usually used when your applications run in standalone containers that need to communicate.
    */
  case object BRIDGE extends NetworkDriver

  /**
    * For standalone containers, remove network isolation between the container and the Docker host,
    * and use the host’s networking directly. host is only available for swarm services on Docker 17.06 and higher.
    */
  case object HOST extends NetworkDriver
}
