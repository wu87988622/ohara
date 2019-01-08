package com.island.ohara.client

import akka.http.scaladsl.model.Multipart.FormData.BodyPart.Strict
import com.island.ohara.client.configurator.v0.ConnectorApi.ConnectorConfiguration
import com.island.ohara.client.configurator.v0.{ConnectorApi, StreamApi}
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

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
}
