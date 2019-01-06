package com.island.ohara.configurator.route
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives.{complete, get, path}
import com.island.ohara.client.ConnectorClient
import com.island.ohara.client.ConnectorJson.Plugin
import com.island.ohara.client.configurator.v0.InfoApi._
import com.island.ohara.common.data.DataType
import com.island.ohara.common.util.VersionUtil
import com.island.ohara.kafka.KafkaClient
import akka.http.scaladsl.server.Directives._
object InfoRoute extends SprayJsonSupport {

  private[this] val SUPPORTED_DATABASES = Seq("mysql")

  def apply(implicit kafkaClient: KafkaClient, connectorClient: ConnectorClient): server.Route =
    // TODO: OHARA-1212 should remove "cluster" ... by chia
    path(INFO_PREFIX_PATH | "cluster") {
      get {
        val plugins = connectorClient.plugins()

        def toConnectorInfo(plugin: Plugin): ConnectorVersion = {
          val (version, revision) = try {
            // see com.island.ohara.kafka.connection.Version for the format from "kafka's version"
            val index = plugin.version.lastIndexOf("_")
            if (index < 0 || index >= plugin.version.length - 1) (plugin.version, "unknown")
            else (plugin.version.substring(0, index), plugin.version.substring(index + 1))
          } catch {
            case _: Throwable => (plugin.version, "unknown")
          }
          ConnectorVersion(plugin.className, version, revision)
        }
        import scala.collection.JavaConverters._
        complete(
          ConfiguratorInfo(
            kafkaClient.brokers,
            connectorClient.workers,
            plugins.filter(_.typeName.toLowerCase == "source").map(toConnectorInfo),
            plugins.filter(_.typeName.toLowerCase == "sink").map(toConnectorInfo),
            SUPPORTED_DATABASES,
            DataType.all.asScala,
            ConfiguratorVersion(
              version = VersionUtil.VERSION,
              user = VersionUtil.USER,
              revision = VersionUtil.REVISION,
              date = VersionUtil.DATE
            )
          ))
      }
    }
}
