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

import com.island.ohara.kafka.connector.json.SettingDefinition
import com.island.ohara.kafka.connector._

import scala.collection.JavaConverters._

/**
  * This class extends RowSinkConnector abstract.
  */
class HDFSSinkConnector extends RowSinkConnector {

  var props: TaskConfig = _

  override protected[hdfs] def _start(config: TaskConfig): Unit = {
    this.props = config
  }

  override protected def _stop(): Unit = {
    //TODO
  }

  override protected def _taskClass(): Class[_ <: RowSinkTask] = classOf[HDFSSinkTask]

  override protected[hdfs] def _taskConfigs(maxTasks: Int): util.List[TaskConfig] = {
    Seq.fill(maxTasks) { props }.asJava
  }

  override protected def _version: ConnectorVersion = ConnectorVersion.DEFAULT

  override protected def _definitions(): util.List[SettingDefinition] = Seq(
    SettingDefinition
      .builder()
      .displayName("HDFS URL")
      .documentation("Input HDFS namenode location")
      .valueType(SettingDefinition.Type.STRING)
      .key(HDFS_URL)
      .build(),
    SettingDefinition
      .builder()
      .displayName("Flush Line Count")
      .documentation("Number of records write to store before invoking file commits.")
      .valueType(SettingDefinition.Type.INT)
      .optional(HDFSSinkConnectorConfig.FLUSH_LINE_COUNT_DEFAULT.toString)
      .key(FLUSH_LINE_COUNT)
      .build(),
    SettingDefinition
      .builder()
      .displayName("Rotate Interval(MS)")
      .documentation("commit file time")
      .valueType(SettingDefinition.Type.LONG)
      .optional(HDFSSinkConnectorConfig.ROTATE_INTERVAL_MS_DEFAULT.toString)
      .key(ROTATE_INTERVAL_MS)
      .build(),
    SettingDefinition
      .builder()
      .displayName("Temp Folder")
      .documentation("write temp data folder")
      .valueType(SettingDefinition.Type.STRING)
      .optional(HDFSSinkConnectorConfig.TMP_DIR_DEFAULT)
      .key(TMP_DIR)
      .build(),
    SettingDefinition
      .builder()
      .displayName("Data Folder")
      .documentation("writer data folder")
      .valueType(SettingDefinition.Type.STRING)
      .optional(HDFSSinkConnectorConfig.DATA_DIR_DEFAULT)
      .key(DATA_DIR)
      .build(),
    SettingDefinition
      .builder()
      .displayName("File Prefix Name")
      .documentation("Data file prefix name")
      .valueType(SettingDefinition.Type.STRING)
      .optional(HDFSSinkConnectorConfig.DATAFILE_PREFIX_NAME_DEFAULT)
      .key(DATAFILE_PREFIX_NAME)
      .build(),
    SettingDefinition
      .builder()
      .displayName("File Need Header")
      .documentation("File need header for flush hdfs data")
      .valueType(SettingDefinition.Type.BOOLEAN)
      .optional(HDFSSinkConnectorConfig.DATAFILE_NEEDHEADER_DEFAULT.toString)
      .key(DATAFILE_NEEDHEADER)
      .build(),
    SettingDefinition
      .builder()
      .displayName("Data File encode")
      .documentation("File encode for write to HDFS file")
      .valueType(SettingDefinition.Type.STRING)
      .optional(HDFSSinkConnectorConfig.DATAFILE_ENCODE_DEFAULT)
      .key(DATAFILE_ENCODE)
      .build()
  ).asJava
}
