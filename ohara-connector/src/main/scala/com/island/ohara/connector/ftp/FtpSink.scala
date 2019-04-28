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
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.kafka.connector._
import com.island.ohara.kafka.connector.json.SettingDefinition

import scala.collection.JavaConverters._

class FtpSink extends RowSinkConnector {
  private[this] var settings: TaskSetting = _
  private[this] var props: FtpSinkProps = _

  override protected[ftp] def _start(settings: TaskSetting): Unit = {
    this.settings = settings
    this.props = FtpSinkProps(settings)
    if (settings.columns.asScala.exists(_.order == 0))
      throw new IllegalArgumentException("column order must be bigger than zero")

    val ftpClient =
      FtpClient.builder().hostname(props.hostname).port(props.port).user(props.user).password(props.password).build()
    try if (!ftpClient.exist(props.outputFolder)) ftpClient.mkdir(props.outputFolder)
    finally ftpClient.close()
  }

  override protected def _stop(): Unit = {
    // do nothing
  }

  override protected def _taskClass(): Class[_ <: RowSinkTask] = classOf[FtpSinkTask]

  override protected def _taskSettings(maxTasks: Int): util.List[TaskSetting] = {
    (0 until maxTasks)
      .map(
        index =>
          settings.append(FtpSinkTaskProps(
            outputFolder = CommonUtils.path(props.outputFolder, s"${settings.id}_$index"),
            needHeader = props.needHeader,
            encode = props.encode,
            hostname = props.hostname,
            port = props.port,
            user = props.user,
            password = props.password
          ).toMap.asJava))
      .asJava
  }

  override protected def _version: ConnectorVersion = ConnectorVersion.DEFAULT

  override protected def _definitions(): util.List[SettingDefinition] = Seq(
    SettingDefinition
      .builder()
      .displayName("output folder")
      .documentation("FTP sink read csv data from topic and then write to this folder")
      .valueType(SettingDefinition.Type.STRING)
      .key(FTP_OUTPUT)
      .build(),
    SettingDefinition
      .builder()
      .displayName("write header")
      .documentation("If true, ftp sink write the header to all output csv file.")
      .valueType(SettingDefinition.Type.BOOLEAN)
      .key(FTP_NEED_HEADER)
      .build(),
    SettingDefinition
      .builder()
      .displayName("csv file encode")
      .documentation("The encode is used to parse input csv files")
      .valueType(SettingDefinition.Type.STRING)
      .key(FTP_ENCODE)
      .optional("UTF-8")
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
