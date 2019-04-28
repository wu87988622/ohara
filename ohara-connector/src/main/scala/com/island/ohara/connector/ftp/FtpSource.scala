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

package com.island.ohara.connector.ftp

import java.util

import com.island.ohara.client.ftp.FtpClient
import com.island.ohara.common.data.Column
import com.island.ohara.kafka.connector.json.SettingDefinition
import com.island.ohara.kafka.connector.{ConnectorVersion, RowSourceConnector, RowSourceTask, TaskSetting}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters._

class FtpSource extends RowSourceConnector {
  private[this] var settings: TaskSetting = _
  private[this] var props: FtpSourceProps = _
  private[this] var schema: Seq[Column] = _

  override protected def _taskClass(): Class[_ <: RowSourceTask] = classOf[FtpSourceTask]

  override protected def _taskSettings(maxTasks: Int): util.List[TaskSetting] = {
    (0 until maxTasks)
      .map(
        index =>
          settings.append(
            FtpSourceTaskProps(
              total = maxTasks,
              hash = index,
              inputFolder = props.inputFolder,
              completedFolder = props.completedFolder,
              errorFolder = props.errorFolder,
              encode = props.encode,
              hostname = props.hostname,
              port = props.port,
              user = props.user,
              password = props.password
            ).toMap.asJava)
      )
      .asJava
  }

  override protected[ftp] def _start(settings: TaskSetting): Unit = {
    this.settings = settings
    this.props = FtpSourceProps(settings)
    this.schema = settings.columns.asScala
    if (schema.exists(_.order == 0)) throw new IllegalArgumentException("column order must be bigger than zero")

    val ftpClient =
      FtpClient.builder().hostname(props.hostname).port(props.port).user(props.user).password(props.password).build()
    try {
      if (ftpClient.nonExist(props.inputFolder))
        throw new IllegalArgumentException(s"${props.inputFolder} doesn't exist")
      if (ftpClient.nonExist(props.errorFolder)) ftpClient.mkdir(props.errorFolder)
      props.completedFolder.foreach(folder => if (ftpClient.nonExist(folder)) ftpClient.mkdir(folder))
    } finally ftpClient.close()
  }

  override protected def _stop(): Unit = {
    //    do nothing
  }

  override protected def _version: ConnectorVersion = ConnectorVersion.DEFAULT

  override protected def _definitions(): util.List[SettingDefinition] = Seq(
    SettingDefinition
      .builder()
      .displayName("input folder")
      .documentation("ftp source connector will load csv file from this folder")
      .valueType(SettingDefinition.Type.STRING)
      .key(FTP_INPUT)
      .build(),
    SettingDefinition
      .builder()
      .displayName("completed folder")
      .documentation("this folder is used to store the completed files. If you don't define a folder," +
        " all completed files will be deleted from ftp server")
      .valueType(SettingDefinition.Type.STRING)
      .optional()
      .key(FTP_COMPLETED_FOLDER)
      .build(),
    SettingDefinition
      .builder()
      .displayName("error folder")
      .documentation("this folder is used to keep the invalid file. For example, non-csv file")
      .valueType(SettingDefinition.Type.STRING)
      .key(FTP_ERROR)
      .build(),
    SettingDefinition
      .builder()
      .displayName("csv file encode")
      .documentation("The encode is used to parse input csv files")
      .valueType(SettingDefinition.Type.STRING)
      .key(FTP_ENCODE)
      .optional(FTP_ENCODE_DEFAULT)
      .build(),
    SettingDefinition
      .builder()
      .displayName("hostname of ftp server")
      .documentation("hostname of ftp server")
      .valueType(SettingDefinition.Type.STRING)
      .key(FTP_HOSTNAME)
      .build(),
    SettingDefinition
      .builder()
      .displayName("port of ftp server")
      .documentation("port of ftp server")
      .valueType(SettingDefinition.Type.INT)
      .key(FTP_PORT)
      .build(),
    SettingDefinition
      .builder()
      .displayName("user of ftp server")
      .documentation(
        "user of ftp server. This account must have read/delete permission of input folder and error folder")
      .valueType(SettingDefinition.Type.STRING)
      .key(FTP_USER_NAME)
      .build(),
    SettingDefinition
      .builder()
      .displayName("password of ftp server")
      .documentation("password of ftp server")
      .valueType(SettingDefinition.Type.PASSWORD)
      .key(FTP_PASSWORD)
      .build(),
  ).asJava
}

object FtpSource {
  val LOG: Logger = LoggerFactory.getLogger(classOf[FtpSource])
}
