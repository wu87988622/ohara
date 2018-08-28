package com.island.ohara.configurator.route
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives.{complete, get, path}
import com.island.ohara.client.ConfiguratorJson.{CLUSTER_PATH, ClusterInformation}
import com.island.ohara.client.ConnectorClient
import com.island.ohara.kafka.KafkaClient

object ClusterRoute extends SprayJsonSupport {

  private[this] val SUPPORTED_DATABASES = Seq("mysql")

  def apply(implicit kafkaClient: KafkaClient, connectorClient: ConnectorClient): server.Route = path(CLUSTER_PATH) {
    get {
      complete(ClusterInformation(kafkaClient.brokers, connectorClient.workers, SUPPORTED_DATABASES))
    }
  }
}
