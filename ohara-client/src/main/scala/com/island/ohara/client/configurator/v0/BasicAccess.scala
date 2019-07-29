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
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.kafka.connector.json.ObjectKey

import scala.concurrent.{ExecutionContext, Future}

/**
  * all accesses in v0 APIs need the hostname and port of remote node. This class implements the basic methods used to store the required
  * information to create the subclass access.
  *
  * @param prefixPath path to remote resource
  */
abstract class BasicAccess private[v0] (prefixPath: String) {
  protected[v0] val exec: HttpExecutor = HttpExecutor.SINGLETON
  // this access is under "v0" package so this field "version" is a constant string.
  private[this] val version = ConfiguratorApiInfo.V0
  private[this] var hostname: String = _
  private[this] var port: Int = -1

  def hostname(hostname: String): BasicAccess.this.type = {
    this.hostname = CommonUtils.requireNonEmpty(hostname)
    this
  }
  def port(port: Int): BasicAccess.this.type = {
    this.port = CommonUtils.requireConnectionPort(port)
    this
  }

  protected def _hostname: String = CommonUtils.requireNonEmpty(hostname)
  protected def _port: Int = CommonUtils.requireConnectionPort(port)
  protected def _version: String = CommonUtils.requireNonEmpty(version)
  protected def _prefixPath: String = CommonUtils.requireNonEmpty(prefixPath)

  /**
    * Compose the url with hostname, port, version and prefix
    * @return url string
    */
  protected def _url: String = s"http://${_hostname}:${_port}/${_version}/${_prefixPath}"

  protected def _url(group: String) =
    s"http://${_hostname}:${_port}/${_version}/${_prefixPath}?${Data.GROUP_KEY}=$group"

  //----------------[for runnable objects]----------------//
  protected def _url(key: ObjectKey) =
    s"http://${_hostname}:${_port}/${_version}/${_prefixPath}/${key.name}?${Data.GROUP_KEY}=${key.group}"

  protected def _url(key: ObjectKey, postFix: String) =
    s"http://${_hostname}:${_port}/${_version}/${_prefixPath}/${key.name}/${CommonUtils.requireNonEmpty(postFix)}?${Data.GROUP_KEY}=${key.group}"

  protected def put(key: ObjectKey, action: String)(implicit executionContext: ExecutionContext): Future[Unit] =
    exec.put[ErrorApi.Error](
      s"http://${_hostname}:${_port}/${_version}/${_prefixPath}/${key.name}/$action?group=${key.group}")
}
