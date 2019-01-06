package com.island.ohara.configurator.route
import akka.http.scaladsl.server
import com.island.ohara.client.configurator.v0.HadoopApi._
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.configurator.Configurator.Store

private[configurator] object HdfsInfoRoute {
  def apply(implicit store: Store): server.Route = RouteUtil.basicRoute[HdfsInfoRequest, HdfsInfo](
    root = HDFS_PREFIX_PATH,
    reqToRes = (id: String, request: HdfsInfoRequest) => HdfsInfo(id, request.name, request.uri, CommonUtil.current())
  )
}
