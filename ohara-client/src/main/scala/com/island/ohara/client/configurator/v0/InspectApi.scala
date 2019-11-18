/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.island.ohara.client.configurator.v0

import java.io.File
import java.util.Objects

import com.island.ohara.client.configurator.v0.FileInfoApi.{ClassInfo, FileInfo}
import com.island.ohara.common.annotations.{Optional, VisibleForTesting}
import com.island.ohara.common.setting.{ObjectKey, SettingDef, TopicKey}
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.kafka.connector.json.ConnectorDefUtils
import com.island.ohara.streams.config.StreamDefUtils
import spray.json.DefaultJsonProtocol._
import spray.json.{JsObject, JsValue, RootJsonFormat}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

object InspectApi {
  val INSPECT_PREFIX_PATH: String = "inspect"
  val RDB_PREFIX_PATH: String = "rdb"
  val TOPIC_PREFIX_PATH: String = "topic"
  val TOPIC_TIMEOUT_KEY: String = "timeout"
  val TOPIC_TIMEOUT_DEFAULT: Duration = 3 seconds
  val TOPIC_LIMIT_KEY: String = "limit"
  val TOPIC_LIMIT_DEFAULT: Int = 5

  val CONFIGURATOR_PREFIX_PATH: String = "configurator"

  //-------------[image]-------------//
  val ZOOKEEPER_PREFIX_PATH: String = "zookeeper"
  val BROKER_PREFIX_PATH: String = "broker"
  val WORKER_PREFIX_PATH: String = "worker"
  val STREAM_PREFIX_PATH: String = "stream"

  //-------------[FILE]-------------//
  val FILE_PREFIX_PATH: String = FileInfoApi.FILE_PREFIX_PATH

  final case class ConfiguratorVersion(version: String, branch: String, user: String, revision: String, date: String)
  implicit val CONFIGURATOR_VERSION_JSON_FORMAT: RootJsonFormat[ConfiguratorVersion] = jsonFormat5(ConfiguratorVersion)

  final case class ConfiguratorInfo(versionInfo: ConfiguratorVersion, mode: String)

  implicit val CONFIGURATOR_INFO_JSON_FORMAT: RootJsonFormat[ConfiguratorInfo] = jsonFormat2(ConfiguratorInfo)

  case class ServiceDefinition(imageName: String, settingDefinitions: Seq[SettingDef], classInfos: Seq[ClassInfo])

  implicit val SERVICE_DEFINITION_FORMAT: RootJsonFormat[ServiceDefinition] = jsonFormat3(ServiceDefinition)

  case class FileContent(classInfos: Seq[ClassInfo]) {
    def sourceClassInfos: Seq[ClassInfo] = classInfos.filter(_.classType == ConnectorDefUtils.SOURCE_CONNECTOR)
    def sinkClassInfos: Seq[ClassInfo] = classInfos.filter(_.classType == ConnectorDefUtils.SINK_CONNECTOR)
    def streamClassInfos: Seq[ClassInfo] = classInfos.filter(_.classType == StreamDefUtils.STREAM)
  }

  object FileContent {
    val empty: FileContent = FileContent(Seq.empty)
  }

  implicit val FILE_CONTENT_FORMAT: RootJsonFormat[FileContent] = new RootJsonFormat[FileContent] {
    private[this] val format = jsonFormat1(FileContent.apply)
    override def write(obj: FileContent): JsValue = JsObject {
      val fields = format.write(obj).asJsObject.fields
      // yep, the APIs compatibility
      fields + ("classes" -> fields("classInfos"))
    }

    override def read(json: JsValue): FileContent = format.read(json)
  }

  final case class RdbColumn(name: String, dataType: String, pk: Boolean)
  implicit val RDB_COLUMN_JSON_FORMAT: RootJsonFormat[RdbColumn] = jsonFormat3(RdbColumn)
  final case class RdbTable(catalogPattern: Option[String],
                            schemaPattern: Option[String],
                            name: String,
                            columns: Seq[RdbColumn])
  implicit val RDB_TABLE_JSON_FORMAT: RootJsonFormat[RdbTable] = jsonFormat4(RdbTable)

  final case class RdbQuery(url: String,
                            user: String,
                            workerClusterKey: ObjectKey,
                            password: String,
                            catalogPattern: Option[String],
                            schemaPattern: Option[String],
                            tableName: Option[String])
  implicit val RDB_QUERY_JSON_FORMAT: OharaJsonFormat[RdbQuery] =
    JsonRefiner[RdbQuery].format(jsonFormat7(RdbQuery)).rejectEmptyString().refine

  final case class RdbInfo(name: String, tables: Seq[RdbTable])
  implicit val RDB_INFO_JSON_FORMAT: RootJsonFormat[RdbInfo] = jsonFormat2(RdbInfo)

  final case class Message(partition: Int,
                           offset: Long,
                           sourceClass: Option[String],
                           sourceKey: Option[ObjectKey],
                           value: Option[JsValue],
                           error: Option[String])
  implicit val MESSAGE_JSON_FORMAT: RootJsonFormat[Message] = jsonFormat6(Message)

  final case class TopicData(messages: Seq[Message])
  implicit val TOPIC_DATA_JSON_FORMAT: RootJsonFormat[TopicData] = jsonFormat1(TopicData)

  /**
    * used to generate the payload and url for POST/PUT request.
    */
  trait RdbRequest {
    def jdbcUrl(url: String): RdbRequest

    @Optional("server will match a broker cluster for you if the wk name is ignored")
    def workerClusterKey(workerClusterKey: ObjectKey): RdbRequest

    def user(user: String): RdbRequest

    def password(password: String): RdbRequest

    @Optional("default is null")
    def catalogPattern(catalogPattern: String): RdbRequest

    @Optional("default is null")
    def schemaPattern(schemaPattern: String): RdbRequest

    @Optional("default is null")
    def tableName(tableName: String): RdbRequest

    @VisibleForTesting
    private[v0] def query: RdbQuery

    /**
      * generate the POST request
      * @param executionContext thread pool
      * @return created data
      */
    def query()(implicit executionContext: ExecutionContext): Future[RdbInfo]
  }

  trait TopicRequest {
    def key(key: TopicKey): TopicRequest
    def limit(limit: Int): TopicRequest
    def timeout(timeout: Duration): TopicRequest
    def query()(implicit executionContext: ExecutionContext): Future[TopicData]
  }

  trait FileRequest {
    def file(file: File): FileRequest
    def query()(implicit executionContext: ExecutionContext): Future[FileInfo]
  }

  final class Access extends BasicAccess(INSPECT_PREFIX_PATH) {

    def configuratorInfo()(implicit executionContext: ExecutionContext): Future[ConfiguratorInfo] =
      exec.get[ConfiguratorInfo, ErrorApi.Error](s"$url/$CONFIGURATOR_PREFIX_PATH")

    def zookeeperInfo()(implicit executionContext: ExecutionContext): Future[ServiceDefinition] =
      exec.get[ServiceDefinition, ErrorApi.Error](s"$url/$ZOOKEEPER_PREFIX_PATH")

    def brokerInfo()(implicit executionContext: ExecutionContext): Future[ServiceDefinition] =
      exec.get[ServiceDefinition, ErrorApi.Error](s"$url/$BROKER_PREFIX_PATH")

    def workerInfo()(implicit executionContext: ExecutionContext): Future[ServiceDefinition] =
      exec.get[ServiceDefinition, ErrorApi.Error](s"$url/$WORKER_PREFIX_PATH")

    def workerInfo(key: ObjectKey)(implicit executionContext: ExecutionContext): Future[ServiceDefinition] =
      exec.get[ServiceDefinition, ErrorApi.Error](urlBuilder.prefix(WORKER_PREFIX_PATH).key(key).build())

    def streamInfo()(implicit executionContext: ExecutionContext): Future[ServiceDefinition] =
      exec.get[ServiceDefinition, ErrorApi.Error](s"$url/$STREAM_PREFIX_PATH")

    def streamInfo(key: ObjectKey)(implicit executionContext: ExecutionContext): Future[ServiceDefinition] =
      exec.get[ServiceDefinition, ErrorApi.Error](urlBuilder.prefix(STREAM_PREFIX_PATH).key(key).build())

    def rdbRequest: RdbRequest = new RdbRequest {
      private[this] var jdbcUrl: String = _
      private[this] var user: String = _
      private[this] var password: String = _
      private[this] var workerClusterKey: ObjectKey = _
      private[this] var catalogPattern: String = _
      private[this] var schemaPattern: String = _
      private[this] var tableName: String = _

      override def jdbcUrl(jdbcUrl: String): RdbRequest = {
        this.jdbcUrl = CommonUtils.requireNonEmpty(jdbcUrl)
        this
      }

      override def workerClusterKey(workerClusterKey: ObjectKey): RdbRequest = {
        this.workerClusterKey = Objects.requireNonNull(workerClusterKey)
        this
      }

      override def user(user: String): RdbRequest = {
        this.user = CommonUtils.requireNonEmpty(user)
        this
      }

      override def password(password: String): RdbRequest = {
        this.password = CommonUtils.requireNonEmpty(password)
        this
      }

      override def catalogPattern(catalogPattern: String): RdbRequest = {
        this.catalogPattern = CommonUtils.requireNonEmpty(catalogPattern)
        this
      }

      override def schemaPattern(schemaPattern: String): RdbRequest = {
        this.schemaPattern = CommonUtils.requireNonEmpty(schemaPattern)
        this
      }

      override def tableName(tableName: String): RdbRequest = {
        this.tableName = CommonUtils.requireNonEmpty(tableName)
        this
      }

      override private[v0] def query: RdbQuery = RdbQuery(
        url = CommonUtils.requireNonEmpty(jdbcUrl),
        user = CommonUtils.requireNonEmpty(user),
        password = CommonUtils.requireNonEmpty(password),
        workerClusterKey = Objects.requireNonNull(workerClusterKey),
        catalogPattern = Option(catalogPattern).map(CommonUtils.requireNonEmpty),
        schemaPattern = Option(schemaPattern).map(CommonUtils.requireNonEmpty),
        tableName = Option(tableName).map(CommonUtils.requireNonEmpty)
      )

      override def query()(implicit executionContext: ExecutionContext): Future[RdbInfo] =
        exec.post[RdbQuery, RdbInfo, ErrorApi.Error](s"$url/$RDB_PREFIX_PATH", query)
    }

    def topicRequest: TopicRequest = new TopicRequest {
      private[this] var key: TopicKey = _
      private[this] var limit = TOPIC_LIMIT_DEFAULT
      private[this] var timeout = TOPIC_TIMEOUT_DEFAULT

      override def key(key: TopicKey): TopicRequest = {
        this.key = Objects.requireNonNull(key)
        this
      }

      override def limit(limit: Int): TopicRequest = {
        this.limit = CommonUtils.requirePositiveInt(limit)
        this
      }

      override def timeout(timeout: Duration): TopicRequest = {
        this.timeout = Objects.requireNonNull(timeout)
        this
      }

      override def query()(implicit executionContext: ExecutionContext): Future[TopicData] =
        exec.post[TopicData, ErrorApi.Error](
          urlBuilder
            .key(key)
            .prefix(TOPIC_PREFIX_PATH)
            .param(TOPIC_LIMIT_KEY, limit.toString)
            .param(TOPIC_TIMEOUT_KEY, timeout.toMillis.toString)
            .build())
    }

    def fileRequest: FileRequest = new FileRequest {
      private[this] var file: File = _

      override def file(file: File): FileRequest = {
        this.file = Objects.requireNonNull(file)
        this
      }

      override def query()(implicit executionContext: ExecutionContext): Future[FileInfo] = {
        FileInfoApi.access.hostname(hostname).port(port).request.file(file).upload()
      }
    }
  }

  def access: Access = new Access
}
