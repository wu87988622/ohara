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
  sealed abstract class InfoAccess extends BasicAccess(INFO_PREFIX_PATH) {
    def get(): Future[ConfiguratorInfo]
  }
  implicit val CONFIGURATOR_INFO_JSON_FORMAT: RootJsonFormat[ConfiguratorInfo] = jsonFormat7(ConfiguratorInfo)

  def access(): InfoAccess = new InfoAccess {
    override def get(): Future[ConfiguratorInfo] =
      exec.get[ConfiguratorInfo](s"http://${_hostname}:${_port}/${_version}/${_prefixPath}")
  }
}
