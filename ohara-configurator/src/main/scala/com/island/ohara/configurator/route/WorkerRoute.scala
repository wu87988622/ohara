package com.island.ohara.configurator.route

import akka.http.scaladsl.server
import com.island.ohara.agent.{BrokerCollie, NodeCollie, WorkerCollie}
import com.island.ohara.client.configurator.v0.WorkerApi._
object WorkerRoute {

  def apply(implicit brokerCollie: BrokerCollie, workerCollie: WorkerCollie, nodeCollie: NodeCollie): server.Route =
    RouteUtil.basicRouteOfCluster(
      root = WORKER_PREFIX_PATH,
      hookOfCreation = (req: WorkerClusterCreationRequest) =>
        workerCollie
          .creator()
          .clusterName(req.name)
          .clientPort(req.clientPort)
          .brokerClusterName(req.brokerClusterName
            .map { bkName =>
              if (!brokerCollie.exists(bkName)) throw new NoSuchElementException(s"$bkName doesn't exist")
              bkName
            }
            .getOrElse {
              if (brokerCollie.size != 1)
                throw new IllegalArgumentException(
                  s"You don't specify the broker cluster for ${req.name}, and there is no default broker cluster")
              brokerCollie.head.name
            })
          .imageName(req.imageName)
          .create(req.nodeNames)
    )
}
