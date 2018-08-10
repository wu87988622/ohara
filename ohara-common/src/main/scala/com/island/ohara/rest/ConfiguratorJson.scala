package com.island.ohara.rest
import com.island.ohara.serialization.DataType
import spray.json.{DefaultJsonProtocol, JsString, JsValue, RootJsonFormat}

/**
  * a collection of marshalling/unmarshalling configurator data to/from json.
  * NOTED: the json format is a part of PUBLIC INTERFACE so please don't change the field names after releasing the ohara.
  * NOTED: common data must be put on the head.
  */
object ConfiguratorJson extends DefaultJsonProtocol {
  //------------------------------------------------[common data]------------------------------------------------//
  /**
    * Provide a way to format DataType. Both Schema and SchemaRrquest use DataType.
    */
  implicit val DATA_TYPE_JSON_FORMAT: RootJsonFormat[DataType] = new RootJsonFormat[DataType] {
    override def write(obj: DataType): JsValue = JsString(obj.name)
    override def read(json: JsValue): DataType = DataType.of(json.asInstanceOf[JsString].value)
  }
  val VERSION_V0 = "v0"
  //------------------------------------------------[storable data]------------------------------------------------//
  val SCHEMA_PATH = "schemas"
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
  implicit val SCHEMA_COMMAND_FORMAT: DataCommandFormat[Schema] = new DataCommandFormat[Schema] {
    override def format(address: String): String = s"http://$address/$VERSION_V0/$SCHEMA_PATH"
    override def format(address: String, uuid: String): String = s"http://$address/$VERSION_V0/$SCHEMA_PATH/$uuid"
  }

  val TOPIC_PATH = "topics"
  final case class TopicInfoRequest(name: String, numberOfPartitions: Int, numberOfReplications: Short)
  implicit val TOPIC_INFO_REQUEST_JSON_FORMAT: RootJsonFormat[TopicInfoRequest] = jsonFormat3(TopicInfoRequest)

  final case class TopicInfo(uuid: String,
                             name: String,
                             numberOfPartitions: Int,
                             numberOfReplications: Short,
                             lastModified: Long)
  implicit val TOPIC_INFO_JSON_FORMAT: RootJsonFormat[TopicInfo] = jsonFormat5(TopicInfo)
  implicit val TOPIC_INFO_COMMAND_FORMAT: DataCommandFormat[TopicInfo] = new DataCommandFormat[TopicInfo] {
    override def format(address: String): String = s"http://$address/$VERSION_V0/$TOPIC_PATH"
    override def format(address: String, uuid: String): String = s"http://$address/$VERSION_V0/$TOPIC_PATH/$uuid"
  }

  val HDFS_PATH = "hdfs"
  final case class HdfsInformationRequest(name: String, uri: String)
  implicit val HDFS_INFORMATION_REQUEST_JSON_FORMAT: RootJsonFormat[HdfsInformationRequest] = jsonFormat2(
    HdfsInformationRequest)

  final case class HdfsInformation(uuid: String, name: String, uri: String, lastModified: Long)
  implicit val HDFS_INFORMATION_JSON_FORMAT: RootJsonFormat[HdfsInformation] = jsonFormat4(HdfsInformation)
  implicit val HDFS_INFORMATION_COMMAND_FORMAT: DataCommandFormat[HdfsInformation] =
    new DataCommandFormat[HdfsInformation] {
      override def format(address: String): String = s"http://$address/$VERSION_V0/$HDFS_PATH"
      override def format(address: String, uuid: String): String = s"http://$address/$VERSION_V0/$HDFS_PATH/$uuid"
    }

  //------------------------------------------------[validation]------------------------------------------------//
  val VALIDATION_PATH = "validate"
  val HDFS_VALIDATION_PATH = "hdfs"
  final case class HdfsValidationRequest(uri: String)
  implicit val HDFS_VALIDATION_REQUEST_JSON_FORMAT: RootJsonFormat[HdfsValidationRequest] = jsonFormat1(
    HdfsValidationRequest)
  implicit val HDFS_VALIDATION_REQUEST_COMMAND_FORMAT: ValidationCommandFormat[HdfsValidationRequest] =
    new ValidationCommandFormat[HdfsValidationRequest] {
      override def format(address: String): String =
        s"http://$address/$VERSION_V0/$VALIDATION_PATH/$HDFS_VALIDATION_PATH"
    }

  val RDB_VALIDATION_PATH = "rdb"
  final case class RdbValidationRequest(uri: String, user: String, password: String)
  implicit val RDB_VALIDATION_REQUEST_JSON_FORMAT: RootJsonFormat[RdbValidationRequest] = jsonFormat3(
    RdbValidationRequest)
  implicit val RDB_VALIDATION_REQUEST_COMMAND_FORMAT: ValidationCommandFormat[RdbValidationRequest] =
    new ValidationCommandFormat[RdbValidationRequest] {
      override def format(address: String): String =
        s"http://$address/$VERSION_V0/$VALIDATION_PATH/$RDB_VALIDATION_PATH"
    }

  final case class ValidationReport(hostname: String, message: String, pass: Boolean)
  implicit val VALIDATION_REPORT_JSON_FORMAT: RootJsonFormat[ValidationReport] = jsonFormat3(ValidationReport)

  //------------------------------------------------[cluster]------------------------------------------------//
  val CLUSTER_PATH = "cluster"
  final case class ClusterInformation(brokers: String, workers: String)
  implicit val CLUSTER_INFORMATION_JSON_FORMAT: RootJsonFormat[ClusterInformation] = jsonFormat2(ClusterInformation)
  implicit val CLUSTER_INFORMATION_COMMAND_FORMAT: ClusterCommandFormat[ClusterInformation] =
    new ClusterCommandFormat[ClusterInformation] {
      override def format(address: String): String = s"http://$address/$VERSION_V0/$CLUSTER_PATH"
    }
  //------------------------------------------------[Error]------------------------------------------------//
  final case class ErrorResponse(code: String, message: String, stack: String)
  implicit val ERROR_RESPONSE_JSON_FORMAT: RootJsonFormat[ErrorResponse] = jsonFormat3(ErrorResponse)
}

/**
  * used to send data command
  */
sealed trait DataCommandFormat[T] {
  def format(address: String): String
  def format(address: String, uuid: String): String
}

/**
  * used to send validation command
  */
sealed trait ValidationCommandFormat[T] {
  def format(address: String): String
}

/**
  * used to send cluster command
  */
sealed trait ClusterCommandFormat[T] {
  def format(address: String): String
}
