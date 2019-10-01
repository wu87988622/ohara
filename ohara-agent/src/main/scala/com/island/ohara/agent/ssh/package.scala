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

package com.island.ohara.agent

import com.island.ohara.agent.docker.NetworkDriver
import com.typesafe.scalalogging.Logger

package object ssh {
  private[ssh] val LOG = Logger(classOf[ServiceCollieImpl])

  /**
    * We need this prefix in order to distinguish our containers from others.
    * DON'T change this constant string. Otherwise, it will break compatibility.
    * We don't use a complex string since docker limit the length of name...
    */
  private[ssh] val PREFIX_KEY = "occl"

  /**
    * used to distinguish the cluster name and service name
    */
  private[ssh] val DIVIDER: String = Collie.DIVIDER

  /**
    * In ssh mode we use host driver to mount /etc/hosts from container host.
    */
  private[ssh] val NETWORK_DRIVER: NetworkDriver = NetworkDriver.HOST
}
