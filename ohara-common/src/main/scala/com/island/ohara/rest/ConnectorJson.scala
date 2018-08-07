package com.island.ohara.rest
import com.sun.xml.internal.ws.encoding.soap.DeserializationException
import spray.json.{DefaultJsonProtocol, JsArray, JsNull, JsObject, JsString, JsValue, RootJsonFormat}

/**
  * a collection of marshalling/unmarshalling connector data to/from json
  */
object ConnectorJson extends DefaultJsonProtocol {
  final case class Plugin(className: String, typeName: String, version: String)

  /**
    * this custom format is necessary since some keys in json are keywords in scala also...
    */
  implicit val PLUGIN_JSON_FORMAT: RootJsonFormat[Plugin] = new RootJsonFormat[Plugin] {
    override def read(value: JsValue): Plugin = {
      value.asJsObject.getFields("class", "type", "version") match {
        case Seq(JsString(className), JsString(typeName), JsString(version)) =>
          Plugin(className, typeName, version)
        case other: Any => throw new DeserializationException(s"${classOf[Plugin].getSimpleName} expected but ${other}")
      }
    }
    override def write(c: Plugin) = JsObject(
      "class" -> JsString(c.className),
      "type" -> JsString(c.typeName),
      "version" -> JsString(c.version)
    )
  }

  /**
    * DON'T change the name of field since it is mapped to the json key
    */
  final case class ConnectorRequest(name: String, config: Map[String, String])
  implicit val CONNECTOR_REQUEST_JSON_FORMAT: RootJsonFormat[ConnectorRequest] = jsonFormat2(ConnectorRequest)

  /**
    * DON'T change the name of field since it is mapped to the json key
    */
  final case class ConnectorResponse(name: String, config: Map[String, String], tasks: Seq[String], typeName: String)

  /**
    * this custom format is necessary since some keys in json are keywords in scala also...
    */
  implicit val CONNECTOR_RESPONSE_JSON_FORMAT: RootJsonFormat[ConnectorResponse] =
    new RootJsonFormat[ConnectorResponse] {
      override def read(value: JsValue): ConnectorResponse = {
        value.asJsObject.getFields("name", "config", "tasks", "type") match {
          case Seq(JsString(className), JsObject(config), JsArray(tasks), JsString(typeName)) =>
            ConnectorResponse(className,
                              config.map { case (k, v) => (k, v.toString()) },
                              tasks.map(_.toString()),
                              typeName)
          // TODO: this is a kafka bug which always returns null in type name. see KAFKA-7253  by chia
          case Seq(JsString(className), JsObject(config), JsArray(tasks), JsNull) =>
            ConnectorResponse(
              className,
              // it is ok to cast JsValue to JsString since we serialize the config to (JsString, JsString)
              config.map { case (k, v) => (k, v.asInstanceOf[JsString].value) },
              tasks.map(_.toString()),
              "null"
            )
          case other: Any =>
            throw new DeserializationException(s"${classOf[ConnectorResponse].getSimpleName} expected but ${other}")
        }
      }
      override def write(c: ConnectorResponse) = JsObject(
        "class" -> JsString(c.name),
        "config" -> JsObject(c.config.map { case (k, v) => (k, JsString(v)) }),
        "tasks" -> JsArray(c.tasks.map(JsString(_)): _*),
        "type" -> JsString(c.typeName)
      )
    }

  /**
    * DON'T change the name of field since it is mapped to the json key
    */
  final case class ErrorResponse(error_code: Int, message: String)
  implicit val ERROR_RESPONSE_JSON_FORMAT: RootJsonFormat[ErrorResponse] = jsonFormat2(ErrorResponse)
}
