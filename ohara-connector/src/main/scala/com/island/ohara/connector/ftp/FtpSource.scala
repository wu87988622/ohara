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
import com.island.ohara.common.setting.SettingDef
import com.island.ohara.kafka.connector.csv.CsvSourceConnector
import com.island.ohara.kafka.connector.{ConnectorVersion, RowSourceTask, TaskSetting}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters._

class FtpSource extends CsvSourceConnector {
  private[this] var settings: TaskSetting = _
  private[this] var props: FtpSourceProps = _
  private[this] var schema: Seq[Column] = _

  override protected def _taskClass(): Class[_ <: RowSourceTask] = classOf[FtpSourceTask]

  override protected def _taskSettings(maxTasks: Int): util.List[TaskSetting] = Seq.fill(maxTasks)(settings).asJava

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

  override protected def _definitions(): util.List[SettingDef] = Seq(
    SettingDef
      .builder()
      .displayName("Hostname")
      .documentation("Hostname of FTP server")
      .valueType(SettingDef.Type.STRING)
      .key(FTP_HOSTNAME)
      .build(),
    SettingDef
      .builder()
      .displayName("Port")
      .documentation("Port of FTP server")
      .valueType(SettingDef.Type.PORT)
      .key(FTP_PORT)
      .build(),
    SettingDef
      .builder()
      .displayName("User")
      .documentation(
        "User of FTP server. This account must have read/delete permission of input folder and error folder")
      .valueType(SettingDef.Type.STRING)
      .key(FTP_USER_NAME)
      .build(),
    SettingDef
      .builder()
      .displayName("Password")
      .documentation("Password of FTP server")
      .valueType(SettingDef.Type.PASSWORD)
      .key(FTP_PASSWORD)
      .build(),
  ).asJava
}

object FtpSource {
  val LOG: Logger = LoggerFactory.getLogger(classOf[FtpSource])
}
