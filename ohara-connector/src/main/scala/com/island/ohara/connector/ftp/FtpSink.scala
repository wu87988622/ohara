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

import com.island.ohara.common.setting.SettingDef
import com.island.ohara.kafka.connector._
import com.island.ohara.kafka.connector.csv.CsvSinkConnector

import scala.collection.JavaConverters._

class FtpSink extends CsvSinkConnector {
  private[this] var settings: TaskSetting = _
  private[this] var props: FtpSinkProps = _

  override protected[ftp] def _start(settings: TaskSetting): Unit = {
    this.settings = settings
    this.props = FtpSinkProps(settings)
    if (settings.columns.asScala.exists(_.order == 0))
      throw new IllegalArgumentException("column order must be bigger than zero")
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
      .displayName("Hostname of FTP Server")
      .documentation("hostname of ftp server")
      .valueType(SettingDef.Type.STRING)
      .key(FTP_HOSTNAME)
      .build(),
    SettingDef
      .builder()
      .displayName("Port of FTP Server")
      .documentation("port of ftp server")
      .valueType(SettingDef.Type.PORT)
      .key(FTP_PORT)
      .build(),
    SettingDef
      .builder()
      .displayName("User of FTP Server")
      .documentation(
        "user of ftp server. This account must have read/delete permission of input folder and error folder")
      .valueType(SettingDef.Type.STRING)
      .key(FTP_USER_NAME)
      .build(),
    SettingDef
      .builder()
      .displayName("Password of FTP Server")
      .documentation("password of ftp server")
      .valueType(SettingDef.Type.PASSWORD)
      .key(FTP_PASSWORD)
      .build(),
  ).asJava
}
