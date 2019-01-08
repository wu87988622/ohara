package com.island.ohara.client.configurator.v0

import com.island.ohara.common.util.CommonUtil

/**
  * all accesses in v0 APIs need the hostname and port of remote node. This class implements the basic methods used to store the required
  * information to create the subclass access.
  * @param prefixPath path to remote resource
  */
abstract class BasicAccess private[v0] (prefixPath: String) {
  protected[v0] val exec: HttpExecutor = HttpExecutor.SINGLETON
  // this access is under "v0" package so this field "version" is a constant string.
  private[this] val version = "v0"
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
