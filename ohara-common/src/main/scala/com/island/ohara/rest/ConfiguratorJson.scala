package com.island.ohara.rest
import com.island.ohara.serialization.DataType
import spray.json.{DefaultJsonProtocol, JsString, JsValue, RootJsonFormat}

/**
  * a collection of marshalling/unmarshalling configurator data to/from json.
  * NOTED: the json format is a part of PUBLIC INTERFACE so please don't change the field names after releasing the ohara.
  */
object ConfiguratorJson extends DefaultJsonProtocol {

  /**
    * Provide a way to format DataType. Both Schema and SchemaRrquest use DataType.
    */
  implicit val DATA_TYPE_JSON_FORMAT: RootJsonFormat[DataType] = new RootJsonFormat[DataType] {
    override def write(obj: DataType): JsValue = JsString(obj.name)
    override def read(json: JsValue): DataType = DataType.of(json.asInstanceOf[JsString].value)
  }

  final case class SchemaRequest(name: String,
                                 types: Map[String, DataType],
                                 orders: Map[String, Int],
                                 disabled: Boolean)

  implicit val SCHEMA_REQUEST_JSON_FORMAT: RootJsonFormat[SchemaRequest] = jsonFormat4(SchemaRequest)

  final case class Schema(uuid: String,
                          name: String,
                          types: Map[String, DataType],
                          orders: Map[String, Int],
                          disabled: Boolean,
                          lastModified: Long)
  implicit val SCHEMA_JSON_FORMAT: RootJsonFormat[Schema] = jsonFormat6(Schema)

  final case class TopicInfoRequest(name: String, numberOfPartitions: Int, numberOfReplications: Short)
  implicit val TOPIC_INFO_REQUEST_JSON_FORMAT: RootJsonFormat[TopicInfoRequest] = jsonFormat3(TopicInfoRequest)

  final case class TopicInfo(uuid: String,
                             name: String,
                             numberOfPartitions: Int,
                             numberOfReplications: Short,
                             lastModified: Long)
  implicit val TOPIC_INFO_JSON_FORMAT: RootJsonFormat[TopicInfo] = jsonFormat5(TopicInfo)

  final case class ErrorResponse(code: String, message: String, stack: String)
  implicit val ERROR_RESPONSE_JSON_FORMAT: RootJsonFormat[ErrorResponse] = jsonFormat3(ErrorResponse)

  final case class ValidationRequest(target: String, uri: String, config: Map[String, String])
  implicit val VALIDATION_REQUEST_JSON_FORMAT: RootJsonFormat[ValidationRequest] = jsonFormat3(ValidationRequest)

  final case class ValidationResponse(hostname: String, message: String, pass: Boolean)
  implicit val VALIDATION_RESPONSE_JSON_FORMAT: RootJsonFormat[ValidationResponse] = jsonFormat3(ValidationResponse)
}
