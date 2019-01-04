package com.island.ohara.configurator.route
import akka.http.scaladsl.server
import com.island.ohara.client.ConfiguratorJson._
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.configurator.Configurator.Store

private[configurator] object HdfsInformationRoute {
  def apply(implicit store: Store): server.Route = RouteUtil.basicRoute[HdfsInformationRequest, HdfsInformation](
    root = HDFS_PATH,
    reqToRes = (id: String, request: HdfsInformationRequest) =>
      HdfsInformation(id, request.name, request.uri, CommonUtil.current())
  )
}
