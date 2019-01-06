package com.island.ohara.client.configurator.v0
import com.island.ohara.common.data.DataType
import spray.json.DefaultJsonProtocol._
import spray.json.{JsString, JsValue, RootJsonFormat}

import scala.concurrent.Future

object InfoApi {

  /**
    * Provide a way to format DataType. Both Schema and Schema request use DataType.
    */
  implicit val DATA_TYPE_JSON_FORMAT: RootJsonFormat[DataType] = new RootJsonFormat[DataType] {
    override def write(obj: DataType): JsValue = JsString(obj.name)
    override def read(json: JsValue): DataType = DataType.of(json.asInstanceOf[JsString].value)
  }
  final case class ConfiguratorVersion(version: String, user: String, revision: String, date: String)
  implicit val CONFIGURATOR_VERSION_JSON_FORMAT: RootJsonFormat[ConfiguratorVersion] = jsonFormat4(ConfiguratorVersion)

  val INFO_PREFIX_PATH: String = "info"

  final case class ConnectorVersion(className: String, version: String, revision: String)
  implicit val CONNECTOR_VERSION_JSON_FORMAT: RootJsonFormat[ConnectorVersion] = jsonFormat3(ConnectorVersion)

  final case class ConfiguratorInfo(brokers: String,
                                    workers: String,
                                    sources: Seq[ConnectorVersion],
                                    sinks: Seq[ConnectorVersion],
                                    supportedDatabases: Seq[String],
                                    supportedDataTypes: Seq[DataType],
                                    versionInfo: ConfiguratorVersion)
  sealed trait InfoAccess {
    def hostname(hostname: String): InfoAccess
    def port(port: Int): InfoAccess
    def get(): Future[ConfiguratorInfo]
  }
  implicit val CONFIGURATOR_INFO_JSON_FORMAT: RootJsonFormat[ConfiguratorInfo] = jsonFormat7(ConfiguratorInfo)

  def access(): InfoAccess = new InfoAccess {
    // the request type is useless here...
    private[this] val access: Access[ConfiguratorInfo, ConfiguratorInfo] =
      new Access[ConfiguratorInfo, ConfiguratorInfo](INFO_PREFIX_PATH)
    override def hostname(hostname: String): InfoAccess = {
      access.hostname(hostname)
      this
    }
    override def port(port: Int): InfoAccess = {
      access.port(port)
      this
    }
    override def get(): Future[ConfiguratorInfo] = access.get()
  }
}
