package com.island.ohara.client.configurator.v0
import spray.json.RootJsonFormat
import spray.json.DefaultJsonProtocol._
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
class Access[Req, Res] private[v0] (prefixPath: String)(implicit rm0: RootJsonFormat[Req], rm1: RootJsonFormat[Res])
    extends BasicAccess(prefixPath) {
  def get(id: String): Future[Res] = exec.get[Res](s"http://${_hostname}:${_port}/${_version}/${_prefixPath}/$id")
  def delete(id: String): Future[Res] = exec.delete[Res](s"http://${_hostname}:${_port}/${_version}/${_prefixPath}/$id")
  def list(): Future[Seq[Res]] = exec.get[Seq[Res]](s"http://${_hostname}:${_port}/${_version}/${_prefixPath}")
  def add(request: Req): Future[Res] =
    exec.post[Req, Res](s"http://${_hostname}:${_port}/${_version}/${_prefixPath}", request)
  def update(id: String, request: Req): Future[Res] =
    exec.put[Req, Res](s"http://${_hostname}:${_port}/${_version}/${_prefixPath}/$id", request)
}
