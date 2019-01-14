package com.island.ohara.configurator.route

import akka.http.scaladsl.server
import com.island.ohara.agent.{BrokerCollie, NodeCollie, ZookeeperCollie}
import com.island.ohara.client.configurator.v0.BrokerApi.{BrokerClusterCreationRequest, _}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
object BrokerRoute {

  def apply(implicit zookeeperCollie: ZookeeperCollie,
            brokerCollie: BrokerCollie,
            nodeCollie: NodeCollie): server.Route = RouteUtil.basicRouteOfCluster(
    root = BROKER_PREFIX_PATH,
    hookOfCreation = (req: BrokerClusterCreationRequest) =>
      // we need to handle the "default name" of zookeeper cluster
      // this is important since user can't assign zk in ohara 0.2 so request won't carry the zk name.
      // we have to reassign the zk name manually.
      // TODO: remove this "friendly" helper in ohara 0.3
      req.zookeeperClusterName
        .map { zkName =>
          zookeeperCollie
            .exists(zkName)
            .flatMap(
              if (_) Future.successful(zkName) else Future.failed(new NoSuchElementException(s"$zkName doesn't exist")))
        }
        .getOrElse {
          zookeeperCollie.clusters().map { clusters =>
            if (clusters.size != 1)
              throw new IllegalArgumentException(
                s"You don't specify the zk cluster for ${req.name}, and there is no default zk cluster")
            clusters.head._1.name
          }
        }
        .flatMap { zkName =>
          brokerCollie
            .creator()
            .clusterName(req.name)
            .clientPort(req.clientPort)
            .zookeeperClusterName(zkName)
            .imageName(req.imageName)
            .nodeNames(req.nodeNames)
            .create()
      }
  )
}
