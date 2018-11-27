package com.island.ohara.agent

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives.{complete, _}
import com.island.ohara.agent.KafkaJson._
import com.island.ohara.common.util.CommonUtil
import spray.json.DefaultJsonProtocol._

object WorkerRoute {
  val WORKERS_SERVICE: String = "workers"
  private[this] val IMAGE_NAME: String = "chia7712/kafka:1.0.2"
  private[this] val ID_KEY: String = "WORKER_GROUP"
  private[this] val OFFSET_TOPIC_KEY: String = "OFFSET_TOPIC"
  private[this] val CONFIG_TOPIC_KEY: String = "CONFIG_TOPIC"
  private[this] val STATUS_TOPIC_KEY: String = "STATUS_TOPIC"
  private[this] val BROKERS_KEY: String = "BROKERS"
  private[this] val PORT_KEY: String = "WORKER_PORT"

  def apply(implicit clusterManager: ClusterManager): server.Route = pathPrefix(WORKERS_SERVICE) {
    path(Segment) { clusterName =>
      get {
        complete(
          ClusterDescription(clusterName, clusterManager.containers(clusterName, WORKERS_SERVICE).flatMap(_._2).toSeq))
      } ~
        delete {
          val cluster = clusterManager.containers(clusterName, WORKERS_SERVICE)
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
        complete(clusterManager.clusters[WorkerCluster]())
      } ~ post {
        entity(as[WorkerCluster]) { cluster =>
          if (clusterManager.existCluster(cluster.name))
            throw new IllegalArgumentException(s"${cluster.name} already exists")
          val brokerCluster = clusterManager.cluster[BrokerCluster](cluster.brokerCluster)
          // add route in order to make broker node can connect to each other.
          val route = cluster.nodes.zipWithIndex.map {
            case (workerNodeName, index) =>
              s"${clusterManager.hostname(WORKERS_SERVICE, index)}" -> CommonUtil.address(workerNodeName)
          }.toMap ++ brokerCluster.nodes.zipWithIndex.map {
            case (brokerNodeName, index) =>
              s"${clusterManager.hostname(BrokerRoute.BROKERS_SERVICE, index)}" -> CommonUtil.address(brokerNodeName)
          }.toMap
          // broker00:8081,broker01:8081
          val brokers = brokerCluster.nodes.zipWithIndex
            .map {
              case (_, index) => s"${clusterManager.hostname(BrokerRoute.BROKERS_SERVICE, index)}:${brokerCluster.port}"
            }
            .mkString(",")
          cluster.nodes.zipWithIndex
            .map {
              case (workerNodeName, index) => clusterManager.node(workerNodeName) -> index
            }
            .foreach {
              case (workerNode, index) =>
                val dockerClient = DockerClient
                  .builder()
                  .hostname(workerNode.hostname)
                  .password(workerNode.password)
                  .user(workerNode.user)
                  .port(workerNode.port)
                  .build()
                try dockerClient
                  .executor()
                  .image(IMAGE_NAME)
                  .portMappings(Map(
                    cluster.port -> cluster.port
                  ))
                  .hostname(clusterManager.hostname(WORKERS_SERVICE, index))
                  .envs(Map(
                    ID_KEY -> cluster.name,
                    PORT_KEY -> cluster.port.toString,
                    BROKERS_KEY -> brokers,
                    CONFIG_TOPIC_KEY -> s"config-topic-${cluster.name}",
                    OFFSET_TOPIC_KEY -> s"offset-topic-${cluster.name}",
                    STATUS_TOPIC_KEY -> s"status-topic-${cluster.name}"
                  ))
                  .name(clusterManager.containerName(cluster.name, WORKERS_SERVICE, index))
                  .route(route)
                  .cleanup()
                  // see docker/kafka.sh for more details
                  .command("worker")
                  .run()
                finally dockerClient.close()
            }
          clusterManager.addCluster(cluster.name, cluster)
          complete(ClusterDescription(cluster.name,
                                      clusterManager.containers(cluster.name, WORKERS_SERVICE).flatMap(_._2).toSeq))
        }
      }
    }
  }
}
