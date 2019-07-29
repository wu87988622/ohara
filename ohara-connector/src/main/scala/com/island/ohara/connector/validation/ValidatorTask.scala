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

package com.island.ohara.connector.validation

import java.util
import java.util.concurrent.TimeUnit

import com.island.ohara.client.configurator.v0.QueryApi.RdbInfo
import com.island.ohara.client.configurator.v0.ValidationApi
import com.island.ohara.client.configurator.v0.ValidationApi.{
  FtpValidation,
  HdfsValidation,
  RdbValidation,
  RdbValidationReport,
  ValidationReport
}
import com.island.ohara.client.database.DatabaseClient
import com.island.ohara.client.ftp.FtpClient
import com.island.ohara.common.data.Serializer
import com.island.ohara.common.util.{CommonUtils, VersionUtils}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.source.{SourceRecord, SourceTask}
import spray.json.{JsObject, JsString}

import scala.collection.JavaConverters._
class ValidatorTask extends SourceTask {
  private[this] var done = false
  private[this] var props: Map[String, String] = _
  private[this] val topic: String = ValidationApi.INTERNAL_TOPIC_KEY.topicNameOnKafka
  private[this] var requestId: String = _
  override def start(props: util.Map[String, String]): Unit = {
    this.props = props.asScala.toMap
    requestId = require(ValidationApi.REQUEST_ID)
  }

  override def poll(): util.List[SourceRecord] = if (done) {
    // just wait the configurator to close this connector
    TimeUnit.SECONDS.sleep(2)
    null
  } else
    try information match {
      case info: HdfsValidation =>
        toSourceRecord(
          ValidationReport(hostname = hostname,
                           message = validate(info),
                           pass = true,
                           lastModified = CommonUtils.current()))
      case info: RdbValidation => toSourceRecord(validate(info))
      case info: FtpValidation =>
        toSourceRecord(
          ValidationReport(hostname = hostname,
                           message = validate(info),
                           pass = true,
                           lastModified = CommonUtils.current()))
    } catch {
      case e: Throwable =>
        toSourceRecord(
          ValidationReport(hostname = hostname,
                           message = e.getMessage,
                           pass = false,
                           lastModified = CommonUtils.current()))
    } finally done = true

  override def stop(): Unit = {
    // do nothing
  }

  override def version(): String = VersionUtils.VERSION

  private[this] def validate(info: HdfsValidation): String = {
    val config = new Configuration()
    config.set("fs.defaultFS", info.uri)
    val fs = FileSystem.get(config)
    val home = fs.getHomeDirectory
    s"check the hdfs:${info.uri} by getting the home:$home"
  }

  private[this] def validate(info: RdbValidation): RdbValidationReport = {
    val client = DatabaseClient.builder.url(info.url).user(info.user).password(info.password).build
    try RdbValidationReport(
      hostname = hostname,
      message = "succeed to fetch table information from database",
      pass = true,
      rdbInfo = RdbInfo(
        client.databaseType,
        client.tables()
      )
    )
    finally client.close()
  }
  private[this] def validate(info: FtpValidation): String = {
    import scala.concurrent.duration._
    val client =
      FtpClient
        .builder()
        .hostname(info.hostname)
        .port(info.port)
        .user(info.user)
        .password(info.password)
        .retryTimeout(5 seconds)
        .build()
    try s"succeed to establish the connection:${info.hostname}:${info.port}. test account:${info.user}" +
      s"by getting working folder:${client.workingFolder()}"
    finally client.close()
  }

  private[this] def toJsObject: JsObject = JsObject(props.map { case (k, v) => (k, JsString(v)) })
  private[this] def information = require(ValidationApi.TARGET) match {
    case ValidationApi.VALIDATION_HDFS_PREFIX_PATH => ValidationApi.HDFS_VALIDATION_JSON_FORMAT.read(toJsObject)
    case ValidationApi.VALIDATION_RDB_PREFIX_PATH  => ValidationApi.RDB_VALIDATION_JSON_FORMAT.read(toJsObject)
    case ValidationApi.VALIDATION_FTP_PREFIX_PATH  => ValidationApi.FTP_VALIDATION_JSON_FORMAT.read(toJsObject)
    case other: String =>
      throw new IllegalArgumentException(s"valid targets are ${ValidationApi.VALIDATION_HDFS_PREFIX_PATH}," +
        s"${ValidationApi.VALIDATION_FTP_PREFIX_PATH} and ${ValidationApi.VALIDATION_HDFS_PREFIX_PATH}. current is $other")
  }

  private[this] def toSourceRecord(data: Object): util.List[SourceRecord] =
    util.Arrays.asList(
      new SourceRecord(null,
                       null,
                       topic,
                       Schema.BYTES_SCHEMA,
                       Serializer.STRING.to(requestId),
                       Schema.BYTES_SCHEMA,
                       Serializer.OBJECT.to(data)))

  private[this] def require(key: String): String =
    props.getOrElse(key, throw new IllegalArgumentException(s"the $key is required"))

  private[this] def hostname: String = try CommonUtils.hostname
  catch {
    case _: Throwable => "unknown"
  }
}
