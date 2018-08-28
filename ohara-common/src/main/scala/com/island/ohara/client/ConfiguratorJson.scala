package com.island.ohara.client
import com.island.ohara.serialization.DataType
import spray.json.{DefaultJsonProtocol, JsBoolean, JsObject, JsString, JsValue, RootJsonFormat}

/**
  * a collection of marshalling/unmarshalling configurator data to/from json.
  * NOTED: the json format is a part of PUBLIC INTERFACE so please don't change the field names after releasing the ohara.
  * NOTED: common data must be put on the head.
  */
object ConfiguratorJson extends DefaultJsonProtocol {
  //------------------------------------------------[COMMON]------------------------------------------------//
  /**
    * Provide a way to format DataType. Both Schema and SchemaRrquest use DataType.
    */
  implicit val DATA_TYPE_JSON_FORMAT: RootJsonFormat[DataType] = new RootJsonFormat[DataType] {
    override def write(obj: DataType): JsValue = JsString(obj.name)
    override def read(json: JsValue): DataType = DataType.of(json.asInstanceOf[JsString].value)
  }
  val VERSION_V0 = "v0"
  val UNKNOWN = "?"

  //------------------------------------------------[DATA]------------------------------------------------//
  sealed trait Data {
    val uuid: String
    val name: String

    /**
      * This field should not be marshalled into json so we make it be "def" rather than "val.
      * DON'T change this filed since it is exposed by restful APIs
      * @return the type of this class
      */
    def kind: String
  }

  /**
    * used to send data command
    */
  sealed trait DataCommandFormat[T] {
    def format(address: String): String
    def format(address: String, uuid: String): String
  }

  //------------------------------------------------[DATA-SCHEMA]------------------------------------------------//
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
      extends Data {
    override def kind: String = "schema"
  }
  implicit val SCHEMA_JSON_FORMAT: RootJsonFormat[Schema] = jsonFormat6(Schema)
  implicit val SCHEMA_COMMAND_FORMAT: DataCommandFormat[Schema] = new DataCommandFormat[Schema] {
    override def format(address: String): String = s"http://$address/$VERSION_V0/$SCHEMA_PATH"
    override def format(address: String, uuid: String): String = s"http://$address/$VERSION_V0/$SCHEMA_PATH/$uuid"
  }

  //------------------------------------------------[DATA-TOPIC]------------------------------------------------//
  val TOPIC_INFO_PATH = "topics"
  final case class TopicInfoRequest(name: String, numberOfPartitions: Int, numberOfReplications: Short)
  implicit val TOPIC_INFO_REQUEST_JSON_FORMAT: RootJsonFormat[TopicInfoRequest] = jsonFormat3(TopicInfoRequest)

  final case class TopicInfo(uuid: String,
                             name: String,
                             numberOfPartitions: Int,
                             numberOfReplications: Short,
                             lastModified: Long)
      extends Data {
    override def kind: String = "topic"
  }
  implicit val TOPIC_INFO_JSON_FORMAT: RootJsonFormat[TopicInfo] = jsonFormat5(TopicInfo)
  implicit val TOPIC_INFO_COMMAND_FORMAT: DataCommandFormat[TopicInfo] = new DataCommandFormat[TopicInfo] {
    override def format(address: String): String = s"http://$address/$VERSION_V0/$TOPIC_INFO_PATH"
    override def format(address: String, uuid: String): String = s"http://$address/$VERSION_V0/$TOPIC_INFO_PATH/$uuid"
  }

  //------------------------------------------------[DATA-HDFS]------------------------------------------------//
  val HDFS_PATH = "hdfs"
  final case class HdfsInformationRequest(name: String, uri: String)
  implicit val HDFS_INFORMATION_REQUEST_JSON_FORMAT: RootJsonFormat[HdfsInformationRequest] = jsonFormat2(
    HdfsInformationRequest)

  final case class HdfsInformation(uuid: String, name: String, uri: String, lastModified: Long) extends Data {
    override def kind: String = "hdfs"
  }
  implicit val HDFS_INFORMATION_JSON_FORMAT: RootJsonFormat[HdfsInformation] = jsonFormat4(HdfsInformation)
  implicit val HDFS_INFORMATION_COMMAND_FORMAT: DataCommandFormat[HdfsInformation] =
    new DataCommandFormat[HdfsInformation] {
      override def format(address: String): String = s"http://$address/$VERSION_V0/$HDFS_PATH"
      override def format(address: String, uuid: String): String = s"http://$address/$VERSION_V0/$HDFS_PATH/$uuid"
    }

  //------------------------------------------------[DATA-PIPELINE]------------------------------------------------//
  sealed abstract class Status extends Serializable {
    def name: String
  }

  object Status {
    case object RUNNING extends Status {
      override def name: String = "running"
    }
    case object STOPPED extends Status {
      override def name: String = "stopped"
    }

    /**
      * @return a array of all supported data type
      */
    def all = Seq(RUNNING, STOPPED)

    /**
      * seek the data type by the type name
      * @param name index of data type
      * @return Data type
      */
    def of(name: String): Status = all.find(_.name.equalsIgnoreCase(name)).get
  }

  implicit val STATUS_JSON_FORMAT: RootJsonFormat[Status] = new RootJsonFormat[Status] {
    override def write(obj: Status): JsValue = JsString(obj.name)
    override def read(json: JsValue): Status = Status.of(json.asInstanceOf[JsString].value)
  }

  val PIPELINE_PATH = "pipelines"

  /**
    * used to control data
    */
  sealed trait ControlCommandFormat[T] {
    def start(address: String, uuid: String): String
    def stop(address: String, uuid: String): String
  }

  final case class PipelineRequest(name: String, rules: Map[String, String])
  implicit val PIPELINE_REQUEST_JSON_FORMAT: RootJsonFormat[PipelineRequest] = jsonFormat2(PipelineRequest)

  final case class ComponentAbstract(uuid: String, name: String, kind: String)
  val UNKNOWN_COMPONENT = ComponentAbstract(UNKNOWN, "unknown", "unknown")
  implicit val COMPONENT_ABSTRACT_JSON_FORMAT: RootJsonFormat[ComponentAbstract] = jsonFormat3(ComponentAbstract)

  final case class Pipeline(uuid: String,
                            name: String,
                            status: Status,
                            rules: Map[String, String],
                            objects: Seq[ComponentAbstract],
                            lastModified: Long)
      extends Data {
    override def kind: String = "pipeline"

    /**
      * @return true if all values have specified uuid. otherwise, false
      */
    def ready(): Boolean = rules.values.exists(!_.equals(UNKNOWN))
  }
  implicit val PIPELINE_JSON_FORMAT: RootJsonFormat[Pipeline] = jsonFormat6(Pipeline)
  implicit val PIPELINE_COMMAND_FORMAT: DataCommandFormat[Pipeline] =
    new DataCommandFormat[Pipeline] {
      override def format(address: String): String = s"http://$address/$VERSION_V0/$PIPELINE_PATH"
      override def format(address: String, uuid: String): String = s"http://$address/$VERSION_V0/$PIPELINE_PATH/$uuid"
    }
  implicit val PIPELINE_CONTROL_FORMAT: ControlCommandFormat[Pipeline] =
    new ControlCommandFormat[Pipeline] {
      override def start(address: String, uuid: String): String =
        s"http://$address/$VERSION_V0/$PIPELINE_PATH/$uuid/start"
      override def stop(address: String, uuid: String): String =
        s"http://$address/$VERSION_V0/$PIPELINE_PATH/$uuid/stop"
    }
  //------------------------------------------------[DATA-SOURCE]------------------------------------------------//
  val SOURCE_PATH = "sources"
  final case class SourceRequest(name: String, configs: Map[String, String])
  implicit val SOURCE_REQUEST_JSON_FORMAT: RootJsonFormat[SourceRequest] = jsonFormat2(SourceRequest)

  final case class Source(uuid: String, name: String, configs: Map[String, String], lastModified: Long) extends Data {
    override def kind: String = "source"
  }
  implicit val SOURCE_JSON_FORMAT: RootJsonFormat[Source] = jsonFormat4(Source)
  implicit val SOURCE_COMMAND_FORMAT: DataCommandFormat[Source] =
    new DataCommandFormat[Source] {
      override def format(address: String): String = s"http://$address/$VERSION_V0/$SOURCE_PATH"
      override def format(address: String, uuid: String): String = s"http://$address/$VERSION_V0/$SOURCE_PATH/$uuid"
    }
  //------------------------------------------------[DATA-SINK]------------------------------------------------//
  val SINK_PATH = "sinks"
  final case class SinkRequest(name: String, configs: Map[String, String])
  implicit val SINK_REQUEST_JSON_FORMAT: RootJsonFormat[SinkRequest] = jsonFormat2(SinkRequest)

  final case class Sink(uuid: String, name: String, configs: Map[String, String], lastModified: Long) extends Data {
    override def kind: String = "sink"
  }
  implicit val SINK_JSON_FORMAT: RootJsonFormat[Sink] = jsonFormat4(Sink)
  implicit val SINK_COMMAND_FORMAT: DataCommandFormat[Sink] =
    new DataCommandFormat[Sink] {
      override def format(address: String): String = s"http://$address/$VERSION_V0/$SINK_PATH"
      override def format(address: String, uuid: String): String = s"http://$address/$VERSION_V0/$SINK_PATH/$uuid"
    }
  //------------------------------------------------[VALIDATION]------------------------------------------------//
  val VALIDATION_PATH = "validate"

  /**
    * used to send validation command
    */
  sealed trait ValidationCommandFormat[T] {
    def format(address: String): String
  }

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

  //------------------------------------------------[QUERY]------------------------------------------------//
  val QUERY_PATH = "query"
  val RDB_QUERY_PATH = "rdb"

  /**
    * used to query 3 party system
    */
  sealed trait QueryCommandFormat[T] {
    def format(address: String): String
  }

  final case class RdbColumn(name: String, typeName: String, pk: Boolean)
  implicit val RDB_COLUMN_JSON_FORMAT: RootJsonFormat[RdbColumn] = new RootJsonFormat[RdbColumn] {
    override def read(json: JsValue): RdbColumn = json.asJsObject.getFields("name", "type", "order", "pk") match {
      case Seq(JsString(n), JsString(t), JsBoolean(pk)) => RdbColumn(n, t, pk)
      case _                                            => throw new UnsupportedOperationException(s"invalid format of ${RdbColumn.getClass.getSimpleName}")
    }
    override def write(obj: RdbColumn): JsValue = JsObject(
      "name" -> JsString(obj.name),
      "type" -> JsString(obj.typeName),
      "pk" -> JsBoolean(obj.pk)
    )
  }
  final case class RdbTable(catalog: String, name: String, columns: Seq[RdbColumn])
  implicit val RDB_TABLE_JSON_FORMAT: RootJsonFormat[RdbTable] = jsonFormat3(RdbTable)

  final case class RdbQuery(url: String,
                            user: String,
                            password: String,
                            catalog: Option[String],
                            tableName: Option[String])
  implicit val RDB_QUERY_JSON_FORMAT: RootJsonFormat[RdbQuery] = jsonFormat5(RdbQuery)
  implicit val RDB_QUERY_COMMAND_FORMAT: QueryCommandFormat[RdbQuery] = new QueryCommandFormat[RdbQuery] {
    override def format(address: String): String = s"http://$address/$VERSION_V0/$QUERY_PATH/$RDB_QUERY_PATH"
  }

  final case class RdbInformation(name: String, tables: Seq[RdbTable])
  implicit val RDB_INFORMATION_JSON_FORMAT: RootJsonFormat[RdbInformation] = jsonFormat2(RdbInformation)

  //------------------------------------------------[CLUSTER]------------------------------------------------//
  val CLUSTER_PATH = "cluster"

  /**
    * used to send cluster command
    */
  sealed trait ClusterCommandFormat[T] {
    def format(address: String): String
  }

  final case class ClusterInformation(brokers: String, workers: String)
  implicit val CLUSTER_INFORMATION_JSON_FORMAT: RootJsonFormat[ClusterInformation] = jsonFormat2(ClusterInformation)
  implicit val CLUSTER_INFORMATION_COMMAND_FORMAT: ClusterCommandFormat[ClusterInformation] =
    new ClusterCommandFormat[ClusterInformation] {
      override def format(address: String): String = s"http://$address/$VERSION_V0/$CLUSTER_PATH"
    }
  //------------------------------------------------[ERROR]------------------------------------------------//
  final case class Error(code: String, message: String, stack: String)
  implicit val ERROR_JSON_FORMAT: RootJsonFormat[Error] = jsonFormat3(Error)
}
