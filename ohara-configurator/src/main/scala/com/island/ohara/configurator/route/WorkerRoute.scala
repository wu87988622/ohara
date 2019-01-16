package com.island.ohara.configurator.route

import akka.http.scaladsl.server
import com.island.ohara.agent.{BrokerCollie, NodeCollie, WorkerCollie}
import com.island.ohara.client.configurator.v0.WorkerApi._
import com.island.ohara.configurator.jar.JarStore

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
object WorkerRoute {

  def apply(implicit brokerCollie: BrokerCollie,
            workerCollie: WorkerCollie,
            nodeCollie: NodeCollie,
            jarStore: JarStore): server.Route =
    RouteUtil.basicRouteOfCluster(
      root = WORKER_PREFIX_PATH,
      hookOfCreation = (req: WorkerClusterCreationRequest) =>
        // we need to handle the "default name" of broker cluster
        // this is important since user can't assign zk in ohara 0.2 so request won't carry the broker name.
        // we have to reassign the broker name manually.
        // TODO: remove this "friendly" helper in ohara 0.3
        req.brokerClusterName
          .map { bkName =>
            brokerCollie
              .exists(bkName)
              .flatMap(if (_) Future.successful(bkName)
              else Future.failed(new NoSuchElementException(s"$bkName doesn't exist")))
          }
          .getOrElse {
            brokerCollie.clusters().map { clusters =>
              if (clusters.size != 1)
                throw new IllegalArgumentException(
                  s"You don't specify the zk cluster for ${req.name}, and there is no default zk cluster")
              clusters.head._1.name
            }
          }
          .flatMap(bkName => jarStore.urls(req.jars).map(bkName -> _))
          .flatMap {
            case (bkName, urls) =>
              workerCollie
                .creator()
                .clusterName(req.name)
                .clientPort(req.clientPort)
                .brokerClusterName(bkName)
                .imageName(req.imageName)
                .jarUrls(urls)
                .nodeNames(req.nodeNames)
                .create()
        }
    )
}
