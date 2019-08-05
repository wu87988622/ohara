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

package com.island.ohara.connector.hdfs.sink

import java.util

import com.island.ohara.common.setting.SettingDef
import com.island.ohara.kafka.connector._

import scala.collection.JavaConverters._

/**
  * This class extends RowSinkConnector abstract.
  */
class HDFSSink extends RowSinkConnector {

  private[this] var settings: TaskSetting = _

  override protected[hdfs] def _start(settings: TaskSetting): Unit = {
    this.settings = settings
  }

  override protected def _stop(): Unit = {
    //TODO
  }

  override protected def _taskClass(): Class[_ <: RowSinkTask] = classOf[HDFSSinkTask]

  override protected[hdfs] def _taskSettings(maxTasks: Int): util.List[TaskSetting] = {
    Seq.fill(maxTasks) { settings }.asJava
  }

  override protected def _version: ConnectorVersion = ConnectorVersion.DEFAULT

  override protected def _definitions(): util.List[SettingDef] = Seq(
    SettingDef
      .builder()
      .displayName("HDFS URL")
      .documentation("Input HDFS namenode location")
      .valueType(SettingDef.Type.STRING)
      .key(HDFS_URL_CONFIG)
      .build(),
    SettingDef
      .builder()
      .displayName("Flush Size")
      .documentation("Number of records write to store before invoking file commits.")
      .valueType(SettingDef.Type.INT)
      .optional(FLUSH_SIZE_DEFAULT.toString)
      .key(FLUSH_SIZE_CONFIG)
      .build(),
    SettingDef
      .builder()
      .displayName("Rotate Interval(MS)")
      .documentation("commit file time")
      .valueType(SettingDef.Type.LONG)
      .optional(ROTATE_INTERVAL_MS_DEFAULT.toString)
      .key(ROTATE_INTERVAL_MS_CONFIG)
      .build(),
    SettingDef
      .builder()
      .displayName("Output Folder")
      .documentation("HDFS sink read csv data from topic and then write to this folder")
      .valueType(SettingDef.Type.STRING)
      .optional(TOPICS_DIR_DEFAULT)
      .key(TOPICS_DIR_CONFIG)
      .build(),
    SettingDef
      .builder()
      .displayName("File Need Header")
      .documentation("File need header for flush hdfs data")
      .valueType(SettingDef.Type.BOOLEAN)
      .optional(FILE_NEED_HEADER_DEFAULT.toString)
      .key(FILE_NEED_HEADER_CONFIG)
      .build(),
    SettingDef
      .builder()
      .displayName("Data File encode")
      .documentation("File encode for write to HDFS file")
      .valueType(SettingDef.Type.STRING)
      .optional(FILE_ENCODE_DEFAULT)
      .key(FILE_ENCODE_CONFIG)
      .build(),
  ).asJava
}
