package com.island.ohara.configurator.route

import akka.http.scaladsl.server
import com.island.ohara.client.configurator.v0.DatabaseApi._
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.configurator.Configurator.Store

private[configurator] object JdbcInfoRoute {
  def apply(implicit store: Store): server.Route = RouteUtil.basicRoute[JdbcInfoRequest, JdbcInfo](
    root = JDBC_PREFIX_PATH,
    reqToRes = (id: String, request: JdbcInfoRequest) =>
      JdbcInfo(id, request.name, request.url, request.user, request.password, CommonUtil.current())
  )
}
