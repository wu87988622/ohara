package com.island.ohara.configurator.route
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives.{complete, get, path}
import com.island.ohara.client.ConfiguratorJson.{CLUSTER_PATH, ClusterInformation, ConnectorInfo}
import com.island.ohara.client.ConnectorClient
import com.island.ohara.kafka.KafkaClient
import com.island.ohara.serialization.DataType

object ClusterRoute extends SprayJsonSupport {

  private[this] val SUPPORTED_DATABASES = Seq("mysql")

  def apply(implicit kafkaClient: KafkaClient, connectorClient: ConnectorClient): server.Route = path(CLUSTER_PATH) {
    get {
      val plugin = connectorClient.plugins()
      val sources =
        plugin.filter(x => x.typeName.toLowerCase() == "source").map(x => ConnectorInfo(x.className, x.version))
      val sinks = plugin.filter(x => x.typeName.toLowerCase() == "sink").map(x => ConnectorInfo(x.className, x.version))

      complete(
        ClusterInformation(kafkaClient.brokers,
                           connectorClient.workers,
                           sources.toSeq,
                           sinks.toSeq,
                           SUPPORTED_DATABASES,
                           DataType.all))
    }
  }
}
