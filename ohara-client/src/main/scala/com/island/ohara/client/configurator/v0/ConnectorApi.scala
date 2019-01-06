package com.island.ohara.client.configurator.v0
import com.island.ohara.common.data.{Column, ConnectorState, DataType}
import spray.json.DefaultJsonProtocol._
import spray.json.{JsNull, JsNumber, JsObject, JsString, JsValue, RootJsonFormat}

object ConnectorApi {
  val CONNECTORS_PREFIX_PATH: String = "connectors"
  final case class PluginDescription(id: String, name: String, size: Long, lastModified: Long) extends Data {
    override def kind: String = "plugin"
  }
  implicit val PLUGIN_JSON_FORMAT: RootJsonFormat[PluginDescription] = jsonFormat4(PluginDescription)
  implicit val COLUMN_JSON_FORMAT: RootJsonFormat[Column] = new RootJsonFormat[Column] {
    override def read(json: JsValue): Column = json.asJsObject.getFields("name", "newName", "dataType", "order") match {
      case Seq(JsString(n), JsString(nn), JsString(t), JsNumber(o)) => Column.of(n, nn, DataType.of(t), o.toInt)
      case Seq(JsString(n), JsNull, JsString(t), JsNumber(o))       => Column.of(n, n, DataType.of(t), o.toInt)
      case Seq(JsString(n), JsString(t), JsNumber(o))               => Column.of(n, n, DataType.of(t), o.toInt)
      case _                                                        => throw new UnsupportedOperationException(s"invalid format from ${classOf[Column].getSimpleName}")
    }
    override def write(obj: Column): JsValue = JsObject(
      "name" -> JsString(obj.name),
      "newName" -> JsString(obj.newName),
      "dataType" -> JsString(obj.dataType.name),
      "order" -> JsNumber(obj.order)
    )
  }
  final case class ConnectorConfigurationRequest(name: String,
                                                 className: String,
                                                 schema: Seq[Column],
                                                 topics: Seq[String],
                                                 numberOfTasks: Int,
                                                 configs: Map[String, String])
  implicit val CONNECTOR_CONFIGURATION_REQUEST_JSON_FORMAT: RootJsonFormat[ConnectorConfigurationRequest] = jsonFormat6(
    ConnectorConfigurationRequest)

  final case class ConnectorConfiguration(id: String,
                                          name: String,
                                          className: String,
                                          schema: Seq[Column],
                                          topics: Seq[String],
                                          numberOfTasks: Int,
                                          configs: Map[String, String],
                                          state: Option[ConnectorState],
                                          lastModified: Long)
      extends Data {
    override def kind: String = className
  }

  implicit val CONNECTOR_STATE_JSON_FORMAT: RootJsonFormat[com.island.ohara.common.data.ConnectorState] =
    new RootJsonFormat[com.island.ohara.common.data.ConnectorState] {
      override def write(obj: com.island.ohara.common.data.ConnectorState): JsValue = JsString(obj.name)
      override def read(json: JsValue): com.island.ohara.common.data.ConnectorState =
        com.island.ohara.common.data.ConnectorState.values
          .find(_.name == json.asInstanceOf[JsString].value)
          .getOrElse(throw new IllegalArgumentException(s"Unknown state name:${json.asInstanceOf[JsString].value}"))
    }

  implicit val CONNECTOR_CONFIGURATION_JSON_FORMAT: RootJsonFormat[ConnectorConfiguration] = jsonFormat9(
    ConnectorConfiguration)
  def access(): Access[ConnectorConfigurationRequest, ConnectorConfiguration] =
    new Access[ConnectorConfigurationRequest, ConnectorConfiguration](CONNECTORS_PREFIX_PATH)
}
