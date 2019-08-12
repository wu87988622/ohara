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
import com.island.ohara.common.setting.SettingDef
import com.island.ohara.kafka.connector._

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

  override protected def _taskSettings(maxTasks: Int): util.List[TaskSetting] = Seq.fill(maxTasks) { settings }.asJava

  override protected def _version: ConnectorVersion = ConnectorVersion.DEFAULT

  override protected def _definitions(): util.List[SettingDef] = Seq(
    SettingDef
      .builder()
      .displayName("output folder")
      .documentation("FTP sink read csv data from topic and then write to this folder")
      .valueType(SettingDef.Type.STRING)
      .key(FTP_OUTPUT)
      .build(),
    SettingDef
      .builder()
      .displayName("write header")
      .documentation("If true, ftp sink write the header to all output csv file.")
      .valueType(SettingDef.Type.BOOLEAN)
      .key(FTP_NEED_HEADER)
      .build(),
    SettingDef
      .builder()
      .displayName("csv file encode")
      .documentation("The encode is used to parse input csv files")
      .valueType(SettingDef.Type.STRING)
      .key(FTP_ENCODE)
      .optional("UTF-8")
      .build(),
    SettingDef
      .builder()
      .displayName("hostname of ftp server")
      .documentation("hostname of ftp server")
      .valueType(SettingDef.Type.STRING)
      .key(FTP_HOSTNAME)
      .build(),
    SettingDef
      .builder()
      .displayName("port of ftp server")
      .documentation("port of ftp server")
      .valueType(SettingDef.Type.PORT)
      .key(FTP_PORT)
      .build(),
    SettingDef
      .builder()
      .displayName("user of ftp server")
      .documentation(
        "user of ftp server. This account must have read/delete permission of input folder and error folder")
      .valueType(SettingDef.Type.STRING)
      .key(FTP_USER_NAME)
      .build(),
    SettingDef
      .builder()
      .displayName("password of ftp server")
      .documentation("password of ftp server")
      .valueType(SettingDef.Type.PASSWORD)
      .key(FTP_PASSWORD)
      .build(),
  ).asJava
}
