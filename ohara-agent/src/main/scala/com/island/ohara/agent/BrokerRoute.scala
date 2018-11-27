package com.island.ohara.agent

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives.{complete, _}
import com.island.ohara.agent.KafkaJson._
import com.island.ohara.common.util.CommonUtil
import spray.json.DefaultJsonProtocol._

object BrokerRoute {
  val BROKERS_SERVICE: String = "brokers"
  private[this] val IMAGE_NAME: String = "chia7712/kafka:1.0.2"
  private[this] val ID_KEY: String = "BROKER_ID"
  private[this] val DATA_DIRECTORY_KEY: String = "BROKER_DATA_DIR"
  private[this] val ZOOKEEPERS_KEY: String = "ZOOKEEPERS"
  private[this] val PORT_KEY: String = "BROKER_PORT"

  def apply(implicit clusterManager: ClusterManager): server.Route = pathPrefix(BROKERS_SERVICE) {
    path(Segment) { clusterName =>
      get {
        complete(
          ClusterDescription(clusterName, clusterManager.containers(clusterName, BROKERS_SERVICE).flatMap(_._2).toSeq))
      } ~
        delete {
          val cluster = clusterManager.containers(clusterName, BROKERS_SERVICE)
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
        complete(clusterManager.clusters[BrokerCluster]())
      } ~ post {
        entity(as[BrokerCluster]) { cluster =>
          if (clusterManager.existCluster(cluster.name))
            throw new IllegalArgumentException(s"${cluster.name} already exists")
          val zkCluster = clusterManager.cluster[ZookeeperCluster](cluster.zookeeperCluster)
          // add route in order to make broker node can connect to each other.
          val route = cluster.nodes.zipWithIndex.map {
            case (brokerNode, index) =>
              s"${clusterManager.hostname(BROKERS_SERVICE, index)}" -> CommonUtil.address(brokerNode)
          }.toMap ++ zkCluster.nodes.zipWithIndex.map {
            case (zkNode, index) =>
              s"${clusterManager.hostname(ZookeeperRoute.ZOOKEEPERS_SERVICE, index)}" -> CommonUtil.address(zkNode)
          }.toMap
          // zk00:2181,zk01:2181
          val zookeepers = zkCluster.nodes.zipWithIndex
            .map {
              case (_, index) =>
                s"${clusterManager.hostname(ZookeeperRoute.ZOOKEEPERS_SERVICE, index)}:${zkCluster.clientPort}"
            }
            .mkString(",")
          cluster.nodes.zipWithIndex
            .map {
              case (brokerNodeName, index) => clusterManager.node(brokerNodeName) -> index
            }
            .foreach {
              case (brokerNode, index) =>
                val dockerClient = DockerClient
                  .builder()
                  .hostname(brokerNode.hostname)
                  .password(brokerNode.password)
                  .user(brokerNode.user)
                  .port(brokerNode.port)
                  .build()
                try dockerClient
                  .executor()
                  .image(IMAGE_NAME)
                  .portMappings(Map(
                    cluster.port -> cluster.port
                  ))
                  .hostname(clusterManager.hostname(BROKERS_SERVICE, index))
                  .envs(Map(
                    ID_KEY -> index.toString,
                    PORT_KEY -> cluster.port.toString,
                    ZOOKEEPERS_KEY -> zookeepers
                  ))
                  .name(clusterManager.containerName(cluster.name, BROKERS_SERVICE, index))
                  .route(route)
                  .cleanup()
                  // see docker/kafka.sh for more details
                  .command("broker")
                  .run()
                finally dockerClient.close()
            }
          clusterManager.addCluster(cluster.name, cluster)
          complete(ClusterDescription(cluster.name,
                                      clusterManager.containers(cluster.name, BROKERS_SERVICE).flatMap(_._2).toSeq))
        }
      }
    }
  }
}
