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

package com.island.ohara.connector.hdfs
import java.util

import com.island.ohara.common.setting.SettingDef
import com.island.ohara.kafka.connector._

import scala.collection.JavaConverters._

/**
  * This class extends RowSinkConnector abstract.
  */
class HDFSSinkConnector extends RowSinkConnector {

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
      .key(HDFS_URL)
      .build(),
    SettingDef
      .builder()
      .displayName("Flush Line Count")
      .documentation("Number of records write to store before invoking file commits.")
      .valueType(SettingDef.Type.INT)
      .optional(HDFSSinkConnectorConfig.FLUSH_LINE_COUNT_DEFAULT.toString)
      .key(FLUSH_LINE_COUNT)
      .build(),
    SettingDef
      .builder()
      .displayName("Rotate Interval(MS)")
      .documentation("commit file time")
      .valueType(SettingDef.Type.LONG)
      .optional(HDFSSinkConnectorConfig.ROTATE_INTERVAL_MS_DEFAULT.toString)
      .key(ROTATE_INTERVAL_MS)
      .build(),
    SettingDef
      .builder()
      .displayName("Temp Folder")
      .documentation("write temp data folder")
      .valueType(SettingDef.Type.STRING)
      .optional(HDFSSinkConnectorConfig.TMP_DIR_DEFAULT)
      .key(TMP_DIR)
      .build(),
    SettingDef
      .builder()
      .displayName("Data Folder")
      .documentation("writer data folder")
      .valueType(SettingDef.Type.STRING)
      .optional(HDFSSinkConnectorConfig.DATA_DIR_DEFAULT)
      .key(DATA_DIR)
      .build(),
    SettingDef
      .builder()
      .displayName("File Prefix Name")
      .documentation("Data file prefix name")
      .valueType(SettingDef.Type.STRING)
      .optional(HDFSSinkConnectorConfig.DATAFILE_PREFIX_NAME_DEFAULT)
      .key(DATAFILE_PREFIX_NAME)
      .build(),
    SettingDef
      .builder()
      .displayName("File Need Header")
      .documentation("File need header for flush hdfs data")
      .valueType(SettingDef.Type.BOOLEAN)
      .optional(HDFSSinkConnectorConfig.DATAFILE_NEEDHEADER_DEFAULT.toString)
      .key(DATAFILE_NEEDHEADER)
      .build(),
    SettingDef
      .builder()
      .displayName("Data File encode")
      .documentation("File encode for write to HDFS file")
      .valueType(SettingDef.Type.STRING)
      .optional(HDFSSinkConnectorConfig.DATAFILE_ENCODE_DEFAULT)
      .key(DATAFILE_ENCODE)
      .build(),
    SettingDef
      .builder()
      .displayName("Data Buffer size")
      .documentation("the size of buffer")
      .valueType(SettingDef.Type.LONG)
      .optional(HDFSSinkConnectorConfig.DATA_BUFFER_COUNT_DEFAULT.toString)
      .key(DATA_BUFFER_COUNT)
      .build(),
    SettingDef
      .builder()
      .displayName("storage class")
      .documentation("the implementation of storage")
      .valueType(SettingDef.Type.CLASS)
      .optional(HDFSSinkConnectorConfig.HDFS_STORAGE_CREATOR_CLASS_DEFAULT)
      .key(HDFS_STORAGE_CREATOR_CLASS)
      .build()
  ).asJava
}
