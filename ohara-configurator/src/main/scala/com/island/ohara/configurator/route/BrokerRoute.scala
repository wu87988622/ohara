package com.island.ohara.configurator.route

import akka.http.scaladsl.server
import com.island.ohara.agent.{BrokerCollie, NodeCollie, ZookeeperCollie}
import com.island.ohara.client.configurator.v0.BrokerApi.{BrokerClusterCreationRequest, _}
object BrokerRoute {

  def apply(implicit zookeeperCollie: ZookeeperCollie,
            brokerCollie: BrokerCollie,
            nodeCollie: NodeCollie): server.Route = RouteUtil.basicRouteOfCluster(
    root = BROKER_PREFIX_PATH,
    hookOfCreation = (req: BrokerClusterCreationRequest) =>
      brokerCollie
        .creator()
        .clusterName(req.name)
        .clientPort(req.clientPort)
        .zookeeperClusterName(req.zookeeperClusterName
          .map { zkName =>
            if (!zookeeperCollie.exists(zkName)) throw new NoSuchElementException(s"$zkName doesn't exist")
            zkName
          }
          .getOrElse {
            if (zookeeperCollie.size != 1)
              throw new IllegalArgumentException(
                s"You don't specify the zk cluster for ${req.name}, and there is no default zk cluster")
            zookeeperCollie.head.name
          })
        .imageName(req.imageName)
        .create(req.nodeNames)
  )
}
