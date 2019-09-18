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

package com.island.ohara.connector.smb

import java.util

import com.island.ohara.client.filesystem.FileSystem
import com.island.ohara.common.data.Column
import com.island.ohara.common.setting.SettingDef
import com.island.ohara.kafka.connector.{RowSourceTask, TaskSetting}
import com.island.ohara.kafka.connector.csv.CsvSourceConnector
import com.island.ohara.kafka.connector.csv.source.CsvSourceConfig

import scala.collection.JavaConverters._

class SmbSource extends CsvSourceConnector {

  private[this] var settings: TaskSetting = _
  private[this] var props: SmbSourceProps = _
  private[this] var schema: Seq[Column] = _

  override protected def _taskClass(): Class[_ <: RowSourceTask] = classOf[SmbSourceTask]

  override protected def _taskSettings(maxTasks: Int): util.List[TaskSetting] = Seq.fill(maxTasks)(settings).asJava

  override protected[smb] def _start(settings: TaskSetting): Unit = {
    this.settings = settings
    this.props = SmbSourceProps(settings)
    this.schema = settings.columns.asScala
    if (schema.exists(_.order == 0)) throw new IllegalArgumentException("column order must be bigger than zero")

    val fileSystem =
      FileSystem.smbBuilder
        .hostname(props.hostname)
        .port(props.port)
        .user(props.user)
        .password(props.password)
        .shareName(props.shareName)
        .build()

    try {
      val csvSourceConfig = CsvSourceConfig.of(settings)

      val inputFolder = csvSourceConfig.inputFolder()
      verifyFolder(inputFolder)
      if (fileSystem.nonExists(inputFolder))
        throw new IllegalArgumentException(s"${inputFolder} doesn't exist")

      val completedFolder = csvSourceConfig.completedFolder()
      if (completedFolder.isPresent) {
        verifyFolder(completedFolder.get())
        if (fileSystem.nonExists(completedFolder.get())) fileSystem.mkdirs(completedFolder.get())
      }

      val errorFolder = csvSourceConfig.errorFolder()
      verifyFolder(errorFolder)
      if (fileSystem.nonExists(errorFolder)) fileSystem.mkdirs(errorFolder)
    } finally fileSystem.close()
  }

  private[this] def verifyFolder(dir: String): Unit = {
    if (dir.startsWith("/")) {
      throw new IllegalArgumentException(s"The $dir is invalid, we don't allow paths beginning with a slash.")
    }
  }

  override protected def _stop(): Unit = {
    //    do nothing
  }

  override protected def _definitions(): util.List[SettingDef] = DEFINITIONS.asJava
}
