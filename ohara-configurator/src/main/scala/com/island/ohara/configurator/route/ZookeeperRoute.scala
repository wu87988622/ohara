package com.island.ohara.configurator.route

import akka.http.scaladsl.server
import com.island.ohara.agent.{NodeCollie, ZookeeperCollie}
import com.island.ohara.client.configurator.v0.ZookeeperApi._

object ZookeeperRoute {

  def apply(implicit zookeeperCollie: ZookeeperCollie, nodeCollie: NodeCollie): server.Route =
    RouteUtil.basicRouteOfCluster(
      root = ZOOKEEPER_PREFIX_PATH,
      hookOfCreation = (req: ZookeeperClusterCreationRequest) =>
        zookeeperCollie
          .creator()
          .clusterName(req.name)
          .clientPort(req.clientPort)
          .electionPort(req.electionPort)
          .peerPort(req.peerPort)
          .imageName(req.imageName)
          .nodeNames(req.nodeNames)
          .create()
    )
}
