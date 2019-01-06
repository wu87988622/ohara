package com.island.ohara.client

import akka.http.scaladsl.model.Multipart.FormData.BodyPart.Strict
import com.island.ohara.client.configurator.v0.ConnectorApi.ConnectorConfiguration
import com.island.ohara.client.configurator.v0.{ConnectorApi, StreamApi}
import spray.json.DefaultJsonProtocol._
import spray.json.{JsNumber, JsObject, JsString, JsValue, RootJsonFormat}

/**
  * a collection from marshalling/unmarshalling configurator data to/from json.
  * NOTED: the json format is a part from PUBLIC INTERFACE so please don't change the field names after releasing the ohara.
  * NOTED: common data must be put on the head.
  */
object ConfiguratorJson {
  //------------------------------------------------[COMMON]------------------------------------------------//

  val VERSION_V0 = "v0"
  val PRIVATE_API = "_private"

  val START_COMMAND: String = "start"
  val STOP_COMMAND: String = "stop"
  val PAUSE_COMMAND: String = "pause"
  val RESUME_COMMAND: String = "resume"
  //------------------------------------------------[DATA]------------------------------------------------//

  /**
    * used to send data command
    */
  sealed trait DataCommandFormat[T] {
    def format(address: String): String
    def format(address: String, id: String): String
  }
  //------------------------------------------------[DATA-PIPELINE]------------------------------------------------//
  /**
    * used to control data
    */
  sealed trait ControlCommandFormat[T] {

    /**
      * used to generate uri to send start request
      *
      * @param address basic address
      * @param id id from data
      * @return uri
      */
    def start(address: String, id: String): String

    /**
      * used to generate uri to send stop request
      *
      * @param address basic address
      * @param id id from data
      * @return uri
      */
    def stop(address: String, id: String): String

    /**
      * used to generate uri to send resume request
      *
      * @param address basic address
      * @param id id from data
      * @return uri
      */
    def resume(address: String, id: String): String

    /**
      * used to generate uri to send pause request
      *
      * @param address basic address
      * @param id id from data
      * @return uri
      */
    def pause(address: String, id: String): String
  }

  //------------------------------------------------[DATA-CONNECTOR]------------------------------------------------//
  implicit val CONNECTOR_CONFIGURATION_CONTROL_FORMAT: ControlCommandFormat[ConnectorConfiguration] =
    new ControlCommandFormat[ConnectorConfiguration] {
      override def start(address: String, id: String): String =
        s"http://$address/$VERSION_V0/${ConnectorApi.CONNECTORS_PREFIX_PATH}/$id/$START_COMMAND"
      override def stop(address: String, id: String): String =
        s"http://$address/$VERSION_V0/${ConnectorApi.CONNECTORS_PREFIX_PATH}/$id/$STOP_COMMAND"
      override def resume(address: String, id: String): String =
        s"http://$address/$VERSION_V0/${ConnectorApi.CONNECTORS_PREFIX_PATH}/$id/$RESUME_COMMAND"
      override def pause(address: String, id: String): String =
        s"http://$address/$VERSION_V0/${ConnectorApi.CONNECTORS_PREFIX_PATH}/$id/$PAUSE_COMMAND"
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
  final case class RdbValidationRequest(url: String, user: String, password: String)
  implicit val RDB_VALIDATION_REQUEST_JSON_FORMAT: RootJsonFormat[RdbValidationRequest] = jsonFormat3(
    RdbValidationRequest)
  implicit val RDB_VALIDATION_REQUEST_COMMAND_FORMAT: ValidationCommandFormat[RdbValidationRequest] =
    new ValidationCommandFormat[RdbValidationRequest] {
      override def format(address: String): String =
        s"http://$address/$VERSION_V0/$VALIDATION_PATH/$RDB_VALIDATION_PATH"
    }

  val FTP_VALIDATION_PATH = "ftp"
  final case class FtpValidationRequest(hostname: String, port: Int, user: String, password: String)
  implicit val FTP_VALIDATION_REQUEST_JSON_FORMAT: RootJsonFormat[FtpValidationRequest] =
    new RootJsonFormat[FtpValidationRequest] {
      override def read(json: JsValue): FtpValidationRequest =
        json.asJsObject.getFields("hostname", "port", "user", "password") match {
          case Seq(JsString(hostname), JsNumber(port), JsString(user), JsString(password)) =>
            FtpValidationRequest(hostname, port.toInt, user, password)
          // we will convert a Map[String, String] to FtpValidationRequest in kafka connector so this method can save us from spray's ClassCastException
          case Seq(JsString(hostname), JsString(port), JsString(user), JsString(password)) =>
            FtpValidationRequest(hostname, port.toInt, user, password)
          case _ =>
            throw new UnsupportedOperationException(
              s"invalid format from ${classOf[FtpValidationRequest].getSimpleName}")
        }

      override def write(obj: FtpValidationRequest): JsValue = JsObject(
        "hostname" -> JsString(obj.hostname),
        "port" -> JsNumber(obj.port),
        "user" -> JsString(obj.user),
        "password" -> JsString(obj.password)
      )
    }

  implicit val FTP_VALIDATION_REQUEST_COMMAND_FORMAT: ValidationCommandFormat[FtpValidationRequest] =
    new ValidationCommandFormat[FtpValidationRequest] {
      override def format(address: String): String =
        s"http://$address/$VERSION_V0/$VALIDATION_PATH/$FTP_VALIDATION_PATH"
    }

  final case class ValidationReport(hostname: String, message: String, pass: Boolean)
  implicit val VALIDATION_REPORT_JSON_FORMAT: RootJsonFormat[ValidationReport] = jsonFormat3(ValidationReport)

  //------------------------------------------------[RDB-QUERY]------------------------------------------------//
  val QUERY_PATH = "query"
  val RDB_PATH = "rdb"

  /**
    * used to query 3 party system
    */
  sealed trait QueryCommandFormat[T] {
    def format(address: String): String
  }

  final case class RdbColumn(name: String, dataType: String, pk: Boolean)
  implicit val RDB_COLUMN_JSON_FORMAT: RootJsonFormat[RdbColumn] = jsonFormat3(RdbColumn)
  final case class RdbTable(catalogPattern: Option[String],
                            schemaPattern: Option[String],
                            name: String,
                            schema: Seq[RdbColumn])
  implicit val RDB_TABLE_JSON_FORMAT: RootJsonFormat[RdbTable] = jsonFormat4(RdbTable)

  final case class RdbQuery(url: String,
                            user: String,
                            password: String,
                            catalogPattern: Option[String],
                            schemaPattern: Option[String],
                            tableName: Option[String])
  implicit val RDB_QUERY_JSON_FORMAT: RootJsonFormat[RdbQuery] = jsonFormat6(RdbQuery)
  implicit val RDB_QUERY_COMMAND_FORMAT: QueryCommandFormat[RdbQuery] = new QueryCommandFormat[RdbQuery] {
    override def format(address: String): String = s"http://$address/$VERSION_V0/$QUERY_PATH/$RDB_PATH"
  }

  final case class RdbInformation(name: String, tables: Seq[RdbTable])
  implicit val RDB_INFORMATION_JSON_FORMAT: RootJsonFormat[RdbInformation] = jsonFormat2(RdbInformation)

  //------------------------------------------------[STREAM]------------------------------------------------//

  // StreamApp List Request Body
  final case class StreamListRequest(jarName: String)
  implicit val STREAM_LIST_REQUEST_JSON_FORMAT: RootJsonFormat[StreamListRequest] = jsonFormat1(StreamListRequest)

  // StreamApp List Page Response Body
  final case class StreamJar(id: String, jarName: String, lastModified: Long)
  implicit val STREAM_LIST_JARS_FORMAT: RootJsonFormat[StreamJar] = jsonFormat3(StreamJar)
  final case class StreamListResponse(jars: Seq[StreamJar])
  implicit val STREAM_LIST_RESPONSE_JSON_FORMAT: RootJsonFormat[StreamListResponse] = jsonFormat1(StreamListResponse)

  // StreamApp List Page Command
  val JARS_STREAM_PATH = "jars"
  implicit val JAR_UPLOAD_LIST_PATH_COMMAND_FORMAT: DataCommandFormat[Strict] =
    new DataCommandFormat[Strict] {
      override def format(address: String): String =
        s"http://$address/$VERSION_V0/${StreamApi.STREAM_PREFIX_PATH}/$JARS_STREAM_PATH"
      override def format(address: String, id: String): String =
        s"http://$address/$VERSION_V0/${StreamApi.STREAM_PREFIX_PATH}/$JARS_STREAM_PATH/$id"
    }
  implicit val LIST_PATH_COMMAND_FORMAT: DataCommandFormat[StreamJar] =
    new DataCommandFormat[StreamJar] {
      override def format(address: String): String =
        s"http://$address/$VERSION_V0/${StreamApi.STREAM_PREFIX_PATH}/$JARS_STREAM_PATH"
      override def format(address: String, id: String): String =
        s"http://$address/$VERSION_V0/${StreamApi.STREAM_PREFIX_PATH}/$JARS_STREAM_PATH/$id"
    }

  //----------------------------------------------------[Cluster]----------------------------------------------------//
  /**
    * used to manipulate the zookeeper/broker/worker cluster.
    * @tparam T type of cluster description
    */
  sealed trait ClusterControlCommand[T] {

    /**
      * used to generate a url to send create request
      *
      * @param address basic address of configurator
      * @return url
      */
    def create(address: String): String

    /**
      * used to generate a url to send remove request
      *
      * @param address basic address of configurator
      * @param name name of cluster
      * @return url
      */
    def remove(address: String, name: String): String

    /**
      * used to generate a url to request the details of cluster.
      * @param address basic address of configurator
      * @param name name of cluster
      * @return url
      */
    def containers(address: String, name: String): String

    /**
      *  used to generate a url to list all cluster
      * @param address basic address of configurator
      * @return url
      */
    def list(address: String): String
  }

  sealed trait ClusterDescription {
    def name: String
    def imageName: String
    def nodeNames: Seq[String]
  }

  //----------------------------------------------------[Zookeeper]----------------------------------------------------//
  val ZOOKEEPER_PATH: String = "zookeepers"
  implicit val ZOOKEEPER_CLUSTER_CONTROL_COMMAND: ClusterControlCommand[ZookeeperClusterDescription] =
    new ClusterControlCommand[ZookeeperClusterDescription] {
      override def create(address: String): String = s"http://$address/$VERSION_V0/$ZOOKEEPER_PATH"
      override def remove(address: String, name: String): String = s"http://$address/$VERSION_V0/$ZOOKEEPER_PATH/$name"
      override def containers(address: String, name: String): String =
        s"http://$address/$VERSION_V0/$ZOOKEEPER_PATH/$name"
      override def list(address: String): String = s"http://$address/$VERSION_V0/$ZOOKEEPER_PATH"
    }

  final case class ZookeeperClusterRequest(name: String,
                                           imageName: Option[String],
                                           clientPort: Option[Int],
                                           peerPort: Option[Int],
                                           electionPort: Option[Int],
                                           nodeNames: Seq[String])

  implicit val ZOOKEEPER_CLUSTER_REQUEST_JSON_FORMAT: RootJsonFormat[ZookeeperClusterRequest] =
    jsonFormat6(ZookeeperClusterRequest)

  final case class ZookeeperClusterDescription(name: String,
                                               imageName: String,
                                               clientPort: Int,
                                               peerPort: Int,
                                               electionPort: Int,
                                               nodeNames: Seq[String])
      extends ClusterDescription

  implicit val ZOOKEEPER_CLUSTER_DESCRIPTION_JSON_FORMAT: RootJsonFormat[ZookeeperClusterDescription] = jsonFormat6(
    ZookeeperClusterDescription)

  //----------------------------------------------------[Broker]----------------------------------------------------//
  final case class BrokerClusterDescription(name: String,
                                            imageName: String,
                                            zookeeperClusterName: String,
                                            clientPort: Int,
                                            nodeNames: Seq[String])
      extends ClusterDescription
  implicit val BROKER_CLUSTER_DESCRIPTION_JSON_FORMAT: RootJsonFormat[BrokerClusterDescription] = jsonFormat5(
    BrokerClusterDescription)

  //----------------------------------------------------[Worker]----------------------------------------------------//
  final case class WorkerClusterDescription(name: String,
                                            imageName: String,
                                            brokerClusterName: String,
                                            clientPort: Int,
                                            groupId: String,
                                            statusTopicName: String,
                                            statusTopicPartitions: Int,
                                            statusTopicReplications: Short,
                                            configTopicName: String,
                                            configTopicPartitions: Int,
                                            configTopicReplications: Short,
                                            offsetTopicName: String,
                                            offsetTopicPartitions: Int,
                                            offsetTopicReplications: Short,
                                            nodeNames: Seq[String])
      extends ClusterDescription
  implicit val WORKER_CLUSTER_DESCRIPTION_JSON_FORMAT: RootJsonFormat[WorkerClusterDescription] = jsonFormat15(
    WorkerClusterDescription)

  //----------------------------------------------------[Docker]----------------------------------------------------//
  /**
    * the enumeration is referenced to container's status. one of created, restarting, running, removing, paused, exited, or dead.
    * see https://docs.docker.com/engine/reference/commandline/ps/#filtering for more information
    *
    */
  abstract sealed class ContainerState extends Serializable {
    // adding a field to display the name from enumeration avoid we break the compatibility when moving code...
    val name: String
  }

  object ContainerState {
    case object CREATED extends ContainerState {
      val name = "CREATED"
    }

    case object RESTARTING extends ContainerState {
      val name = "RESTARTING"
    }

    case object RUNNING extends ContainerState {
      val name = "RUNNING"
    }

    case object REMOVING extends ContainerState {
      val name = "REMOVING"
    }

    case object PAUSED extends ContainerState {
      val name = "PAUSED"
    }

    case object EXITED extends ContainerState {
      val name = "EXITED"
    }

    case object DEAD extends ContainerState {
      val name = "DEAD"
    }

    val all: Seq[ContainerState] = Seq(
      CREATED,
      RESTARTING,
      RUNNING,
      REMOVING,
      PAUSED,
      EXITED,
      DEAD
    )
  }
  implicit val CONTAINER_STATE_JSON_FORMAT: RootJsonFormat[ContainerState] = new RootJsonFormat[ContainerState] {
    override def write(obj: ContainerState): JsValue = JsString(obj.name)
    override def read(json: JsValue): ContainerState = ContainerState.all
      .find(_.name == json.asInstanceOf[JsString].value)
      .getOrElse(throw new IllegalArgumentException(s"Unknown state name:${json.asInstanceOf[JsString].value}"))
  }

  final case class PortPair(hostPort: Int, containerPort: Int)
  implicit val PORT_PAIR_JSON_FORMAT: RootJsonFormat[PortPair] = jsonFormat2(PortPair)

  final case class PortMapping(hostIp: String, portPairs: Seq[PortPair])
  implicit val PORT_MAPPING_JSON_FORMAT: RootJsonFormat[PortMapping] = jsonFormat2(PortMapping)

  final case class ContainerDescription(nodeName: String,
                                        id: String,
                                        imageName: String,
                                        created: String,
                                        state: ContainerState,
                                        name: String,
                                        size: String,
                                        portMappings: Seq[PortMapping],
                                        environments: Map[String, String],
                                        hostname: String)
  implicit val CONTAINER_DESCRIPTION_JSON_FORMAT: RootJsonFormat[ContainerDescription] = jsonFormat10(
    ContainerDescription)
}
