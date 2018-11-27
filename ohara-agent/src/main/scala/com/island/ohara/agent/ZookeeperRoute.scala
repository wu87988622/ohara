package com.island.ohara.agent

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives.{complete, _}
import com.island.ohara.common.util.CommonUtil
import spray.json.DefaultJsonProtocol._
import KafkaJson._

object ZookeeperRoute {
  val ZOOKEEPERS_SERVICE: String = "zookeepers"
  private[this] val IMAGE_NAME: String = "chia7712/zookeeper:3.4.13"
  private[this] val PORT_KEY: String = "ZK_PORT"
  private[this] val DATA_DIRECTORY_KEY: String = "ZK_DATA_DIR"
  private[this] val SERVERS_KEY: String = "ZK_SERVERS"
  private[this] val ID_KEY: String = "ZK_ID"

  def apply(implicit clusterManager: ClusterManager): server.Route = pathPrefix(ZOOKEEPERS_SERVICE) {
    path(Segment) { clusterName =>
      get {
        complete(
          ClusterDescription(clusterName,
                             clusterManager.containers(clusterName, ZOOKEEPERS_SERVICE).flatMap(_._2).toSeq))
      } ~
        delete {
          val cluster = clusterManager.containers(clusterName, ZOOKEEPERS_SERVICE)
          cluster.foreach {
            case (node, containers) =>
              val dockerClient =
                DockerClient
                  .builder()
                  .hostname(node.hostname)
                  .password(node.password)
                  .user(node.user)
                  .port(node.port)
                  .build()
              try containers.foreach(c => dockerClient.stop(c.name))
              finally dockerClient.close()
          }
          clusterManager.removeCluster(clusterName)
          complete(ClusterDescription(clusterName, cluster.flatMap(_._2).toSeq))
        }
    } ~ pathEnd {
      get {
        complete(clusterManager.clusters[ZookeeperCluster]())
      } ~ post {
        entity(as[ZookeeperCluster]) { cluster =>
          if (clusterManager.existCluster(cluster.name))
            throw new IllegalArgumentException(s"${cluster.name} already exists")

          // server.0=zk00:2888:3888 server.1=zk01:2888:3888
          val servers = cluster.nodes.zipWithIndex
            .map {
              case (_, index) =>
                s"server.$index=${clusterManager.hostname(ZOOKEEPERS_SERVICE, index)}:${cluster.followerPort}:${cluster.electionPort}"
            }
            .mkString(" ")
          // add route in order to make zk node can connect to each other.
          val route = cluster.nodes.zipWithIndex.map {
            case (zkNodeName, index) =>
              s"${clusterManager.hostname(ZOOKEEPERS_SERVICE, index)}" -> CommonUtil.address(
                clusterManager.node(zkNodeName).hostname)
          }.toMap
          cluster.nodes.zipWithIndex
            .map {
              case (zkNodeName, index) => clusterManager.node(zkNodeName) -> index
            }
            .foreach {
              case (zkNode, index) =>
                val dockerClient = DockerClient
                  .builder()
                  .hostname(zkNode.hostname)
                  .password(zkNode.password)
                  .user(zkNode.user)
                  .port(zkNode.port)
                  .build()
                try dockerClient
                  .executor()
                  .image(IMAGE_NAME)
                  .portMappings(
                    Map(
                      cluster.clientPort -> cluster.clientPort,
                      cluster.followerPort -> cluster.followerPort,
                      cluster.electionPort -> cluster.electionPort
                    ))
                  .hostname(clusterManager.hostname(ZOOKEEPERS_SERVICE, index))
                  .envs(Map(
                    ID_KEY -> index.toString,
                    PORT_KEY -> cluster.clientPort.toString,
                    SERVERS_KEY -> servers
                  ))
                  .name(clusterManager.containerName(cluster.name, ZOOKEEPERS_SERVICE, index))
                  .route(route)
                  .cleanup()
                  .run()
                finally dockerClient.close()
            }
          clusterManager.addCluster(cluster.name, cluster)
          complete(ClusterDescription(cluster.name,
                                      clusterManager.containers(cluster.name, ZOOKEEPERS_SERVICE).flatMap(_._2).toSeq))
        }
      }
    }
  }
}
