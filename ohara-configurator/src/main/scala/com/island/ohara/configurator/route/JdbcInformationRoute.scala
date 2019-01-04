package com.island.ohara.configurator.route

import akka.http.scaladsl.server
import com.island.ohara.client.ConfiguratorJson._
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.configurator.Configurator.Store

private[configurator] object JdbcInformationRoute {
  def apply(implicit store: Store): server.Route = RouteUtil.basicRoute[JdbcInformationRequest, JdbcInformation](
    root = JDBC_PATH,
    reqToRes = (id: String, request: JdbcInformationRequest) =>
      JdbcInformation(id, request.name, request.url, request.user, request.password, CommonUtil.current())
  )
}
