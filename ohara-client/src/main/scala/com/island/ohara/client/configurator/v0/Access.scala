package com.island.ohara.client.configurator.v0
import com.island.ohara.common.util.CommonUtil
import spray.json.RootJsonFormat

import scala.concurrent.Future

/**
  * A general class used to access data of configurator. The protocol is based on http (restful APIs), and this implementation is built by akka
  * http. All data in ohara have same APIs so we extract this layer to make our life easily.
  * @param prefixPath path to data
  * @param rm0 formatter of request
  * @param rm1 formatter of response
  * @tparam Req type of request
  * @tparam Res type of Response
  */
class Access[Req, Res] private[v0] (prefixPath: String)(implicit rm0: RootJsonFormat[Req], rm1: RootJsonFormat[Res]) {
  private[this] val exec = HttpExecutor.SINGLETON
  // this access is under "v0" package so this field "version" is a constant string.
  private[this] val version = "v0"
  private[this] var hostname: String = _
  private[this] var port: Int = -1

  def hostname(hostname: String): Access[Req, Res] = {
    this.hostname = hostname
    this
  }
  def port(port: Int): Access[Req, Res] = {
    this.port = port
    this
  }

  private[this] def _hostname: String = CommonUtil.requireNonEmpty(hostname, () => "hostname can't be empty")
  private[this] def _port: Int = CommonUtil.requirePositiveNumber(port, () => "port can't be empty")
  private[this] def _version: String = CommonUtil.requireNonEmpty(version, () => "version can't be empty")
  private[this] def _prefixPath: String = CommonUtil.requireNonEmpty(prefixPath, () => "prefixPath can't be empty")

  // TODO: this is specific case in info route...How to integrate them? by chia
  private[v0] def get(): Future[Res] = exec.get[Res](s"http://${_hostname}:${_port}/${_version}/${_prefixPath}")
  def get(id: String): Future[Res] = exec.get[Res](s"http://${_hostname}:${_port}/${_version}/${_prefixPath}/$id")
  def delete(id: String): Future[Res] = exec.delete[Res](s"http://${_hostname}:${_port}/${_version}/${_prefixPath}/$id")
  def list(): Future[Seq[Res]] = exec.get2[Res](s"http://${_hostname}:${_port}/${_version}/${_prefixPath}")
  def add(request: Req): Future[Res] =
    exec.post[Res, Req](s"http://${_hostname}:${_port}/${_version}/${_prefixPath}", request)
  def update(id: String, request: Req): Future[Res] =
    exec.put[Res, Req](s"http://${_hostname}:${_port}/${_version}/${_prefixPath}/$id", request)
}
