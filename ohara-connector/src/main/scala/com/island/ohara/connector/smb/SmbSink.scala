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
import com.island.ohara.common.setting.SettingDef
import com.island.ohara.kafka.connector.csv.sink.CsvSinkConfig
import com.island.ohara.kafka.connector.csv.CsvSinkConnector
import com.island.ohara.kafka.connector.{RowSinkTask, TaskSetting}

import scala.collection.JavaConverters._

class SmbSink extends CsvSinkConnector {
  private[this] var settings: TaskSetting = _

  override protected def _taskClass(): Class[_ <: RowSinkTask] = classOf[SmbSinkTask]

  override protected def _taskSettings(maxTasks: Int): util.List[TaskSetting] = Seq.fill(maxTasks)(settings).asJava

  override protected[smb] def _start(settings: TaskSetting): Unit = {
    this.settings = settings
    val props = SmbProps(settings)
    val schema = settings.columns.asScala
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
      val csvSinkConfig = CsvSinkConfig.of(settings, settings.columns)
      val topicsDir = csvSinkConfig.topicsDir()
      if (topicsDir.startsWith("/")) {
        throw new IllegalArgumentException(s"The $topicsDir is invalid, we don't allow paths beginning with a slash.")
      }
      if (fileSystem.nonExists(topicsDir)) {
        throw new IllegalArgumentException(s"${topicsDir} doesn't exist")
      }
    } finally fileSystem.close()
  }

  override protected def _stop(): Unit = {
    //    do nothing
  }

  override protected def _definitions(): util.List[SettingDef] = DEFINITIONS.asJava
}
