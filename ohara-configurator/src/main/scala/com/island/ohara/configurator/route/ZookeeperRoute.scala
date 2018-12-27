package com.island.ohara.configurator.route

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import com.island.ohara.agent.{NodeCollie, ZookeeperCollie}
import com.island.ohara.client.ConfiguratorJson._
import spray.json.DefaultJsonProtocol._
import scala.util.{Failure, Success}

object ZookeeperRoute {

  def apply(implicit zookeeperCollie: ZookeeperCollie, nodeCollie: NodeCollie): server.Route =
    pathPrefix(ZOOKEEPER_PATH) {
      pathEnd {
        // add
        post {
          entity(as[ZookeeperClusterRequest]) { req =>
            if (req.nodeNames.isEmpty) throw new IllegalArgumentException(s"You are too poor to buy any server?")
            if (zookeeperCollie.exists(req.name)) throw new IllegalArgumentException(s"${req.name} exists!!!")
            onComplete(
              zookeeperCollie
                .creator()
                .clusterName(req.name)
                .clientPort(req.clientPort)
                .electionPort(req.electionPort)
                .peerPort(req.peerPort)
                .imageName(req.imageName)
                .create(req.nodeNames)) {
              case Success(cluster) => complete(cluster)
              case Failure(ex)      => throw ex
            }
          }
        } ~ get(complete(zookeeperCollie.toSeq))
      } ~ path(Segment) { clusterName =>
        delete {
          onComplete(zookeeperCollie.remove(clusterName)) {
            case Success(cluster) => complete(cluster)
            case Failure(ex)      => throw ex
          }
        } ~ get {
          complete(zookeeperCollie.containers(clusterName))
        }
      }
    }
}
