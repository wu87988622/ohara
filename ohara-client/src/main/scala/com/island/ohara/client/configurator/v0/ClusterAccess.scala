package com.island.ohara.client.configurator.v0
import com.island.ohara.client.configurator.v0.ContainerApi._
import com.island.ohara.common.util.CommonUtil
import spray.json.RootJsonFormat
import spray.json.DefaultJsonProtocol._

import scala.concurrent.Future

/**
  * the cluster-related data is different from normal data so we need another type of access.
  * @param prefixPath path to remote resource
  */
class ClusterAccess[Req, Res <: ClusterInfo] private[v0] (prefixPath: String)(implicit rm0: RootJsonFormat[Req],
                                                                              rm1: RootJsonFormat[Res])
    extends BasicAccess(prefixPath) {

  private[this] def _clusterName(name: String): String =
    CommonUtil.requireNonEmpty(name, () => "cluster name can't be empty")
  private[this] def _nodeName(name: String): String = CommonUtil.requireNonEmpty(name, () => "node name can't be empty")

  def get(clusterName: String): Future[Seq[ContainerInfo]] =
    exec.get[Seq[ContainerInfo]](
      s"http://${_hostname}:${_port}/${_version}/${_prefixPath}/${_clusterName(clusterName)}")
  def delete(clusterName: String): Future[Res] =
    exec.delete[Res](s"http://${_hostname}:${_port}/${_version}/${_prefixPath}/$clusterName")
  def list(): Future[Seq[Res]] = exec.get[Seq[Res]](s"http://${_hostname}:${_port}/${_version}/${_prefixPath}")
  def add(request: Req): Future[Res] =
    exec.post[Req, Res](s"http://${_hostname}:${_port}/${_version}/${_prefixPath}", request)
  def addNode(clusterName: String, nodeName: String): Future[Res] =
    exec.post[Res](
      s"http://${_hostname}:${_port}/${_version}/${_prefixPath}/${_clusterName(clusterName)}/${_nodeName(nodeName)}")
  def removeNode(clusterName: String, nodeName: String): Future[Res] =
    exec.delete[Res](
      s"http://${_hostname}:${_port}/${_version}/${_prefixPath}/${_clusterName(clusterName)}/${_nodeName(nodeName)}")
}
