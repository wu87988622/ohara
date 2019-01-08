package com.island.ohara.configurator.route

import akka.http.scaladsl.server
import com.island.ohara.agent.ClusterCollie
import com.island.ohara.client.configurator.v0.NodeApi._
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.configurator.Configurator.Store
object NodesRoute {

  private[this] def update(res: Node)(implicit clusterCollie: ClusterCollie): Node = res.copy(
    services = Seq(
      NodeService(
        name = "zookeeper",
        clusterNames = clusterCollie.zookeepersCollie().filter(_.nodeNames.contains(res.name)).map(_.name).toSeq
      ),
      NodeService(
        name = "broker",
        clusterNames = clusterCollie.brokerCollie().filter(_.nodeNames.contains(res.name)).map(_.name).toSeq
      ),
      NodeService(
        name = "connect-worker",
        clusterNames = clusterCollie.workerCollie().filter(_.nodeNames.contains(res.name)).map(_.name).toSeq
      )
    )
  )

  def apply(implicit store: Store, clusterCollie: ClusterCollie): server.Route =
    RouteUtil.basicRoute[NodeCreationRequest, Node](
      root = NODES_PREFIX_PATH,
      hookOfAdd = (_: String, request: NodeCreationRequest) => {
        if (request.name.isEmpty) throw new IllegalArgumentException(s"name is required")
        update(
          Node(
            name = request.name.get,
            port = request.port,
            user = request.user,
            password = request.password,
            services = Seq.empty,
            lastModified = CommonUtil.current()
          ))
      },
      hookOfUpdate = (name: String, request: NodeCreationRequest, previous: Node) => {
        if (request.name.exists(_ != name))
          throw new IllegalArgumentException(
            s"the name from request is conflict with previous setting:${previous.name}")
        update(
          Node(
            name = name,
            port = request.port,
            user = request.user,
            password = request.password,
            services = Seq.empty,
            lastModified = CommonUtil.current()
          ))
      },
      hookOfGet = (response: Node) => response,
      hookOfList = (responses: Seq[Node]) => responses,
      hookBeforeDelete = (id: String) => id,
      hookOfDelete = (response: Node) => response
    )
}
