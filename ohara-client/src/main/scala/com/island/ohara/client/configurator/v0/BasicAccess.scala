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

import com.island.ohara.client.HttpExecutor
import com.island.ohara.client.configurator.ConfiguratorApiInfo
import com.island.ohara.common.util.CommonUtil

/**
  * all accesses in v0 APIs need the hostname and port of remote node. This class implements the basic methods used to store the required
  * information to create the subclass access.
  * @param prefixPath path to remote resource
  */
abstract class BasicAccess private[v0] (prefixPath: String) {
  protected[v0] val exec: HttpExecutor = HttpExecutor.SINGLETON
  // this access is under "v0" package so this field "version" is a constant string.
  private[this] val version = ConfiguratorApiInfo.V0
  private[this] var hostname: String = _
  private[this] var port: Int = -1

  def hostname(hostname: String): BasicAccess.this.type = {
    this.hostname = hostname
    this
  }
  def port(port: Int): BasicAccess.this.type = {
    this.port = port
    this
  }

  protected def _hostname: String = CommonUtil.requireNonEmpty(hostname, () => "hostname can't be empty")
  protected def _port: Int = CommonUtil.requirePositiveInt(port, () => "port can't be empty")
  protected def _version: String = CommonUtil.requireNonEmpty(version, () => "version can't be empty")
  protected def _prefixPath: String = CommonUtil.requireNonEmpty(prefixPath, () => "prefixPath can't be empty")
}
