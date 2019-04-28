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

import com.island.ohara.connector.hdfs.creator.HDFSStorageCreator
import com.island.ohara.kafka.connector.TaskSetting
import org.apache.hadoop.conf.Configuration

case class HDFSSinkConnectorConfig(hdfsURL: String,
                                   flushLineCount: Int,
                                   rotateIntervalMS: Long,
                                   tmpDir: String,
                                   dataDir: String,
                                   dataFilePrefixName: String,
                                   dataFileNeedHeader: Boolean,
                                   dataBufferCount: Long,
                                   hdfsStorageCreateClass: String,
                                   dataFileEncode: String) {
  def toMap: Map[String, String] = Map(
    HDFS_URL -> hdfsURL,
    FLUSH_LINE_COUNT -> flushLineCount.toString,
    ROTATE_INTERVAL_MS -> rotateIntervalMS.toString,
    TMP_DIR -> tmpDir,
    DATA_DIR -> dataDir,
    DATAFILE_PREFIX_NAME -> dataFilePrefixName,
    DATAFILE_NEEDHEADER -> dataFileNeedHeader.toString,
    DATA_BUFFER_COUNT -> dataBufferCount.toString,
    HDFS_STORAGE_CREATOR_CLASS -> hdfsStorageCreateClass
  )

  def hadoopConfiguration(): Configuration = {
    val config = new Configuration()
    config.set(HadoopConfigurationConstants.FS_DEFAULTFS, hdfsURL)
    config
  }
}

object HDFSSinkConnectorConfig {
  private[hdfs] val FLUSH_LINE_COUNT_DEFAULT: Int = 1000
  private[hdfs] val ROTATE_INTERVAL_MS_DEFAULT: Long = 60000
  private[hdfs] val TMP_DIR_DEFAULT: String = "/tmp"
  private[hdfs] val DATA_DIR_DEFAULT = "/data"
  private[hdfs] val DATAFILE_NEEDHEADER_DEFAULT: Boolean = true
  private[hdfs] val DATAFILE_PREFIX_NAME_DEFAULT: String = "part"
  private[hdfs] val DATA_BUFFER_COUNT_DEFAULT: Long = 100
  private[hdfs] val HDFS_STORAGE_CREATOR_CLASS_DEFAULT: String = classOf[HDFSStorageCreator].getName
  private[hdfs] val DATAFILE_ENCODE_DEFAULT = "UTF-8"

  def apply(settings: TaskSetting): HDFSSinkConnectorConfig = {
    val prefixFileName: String = settings.stringOption(DATAFILE_PREFIX_NAME).orElse(DATAFILE_PREFIX_NAME_DEFAULT)
    if (!prefixFileName.matches(PREFIX_FILENAME_PATTERN)) {
      throw new IllegalArgumentException("The " + DATAFILE_PREFIX_NAME + " value only a-z or A-Z or 0-9")
    }

    val tmpDir: String = settings.stringOption(TMP_DIR).orElse(TMP_DIR_DEFAULT)
    val dataDir: String = settings.stringOption(DATA_DIR).orElse(DATA_DIR_DEFAULT)
    if (tmpDir == dataDir)
      throw new IllegalArgumentException("The tmpDir path same as dataDir path, Please input different path.")

    HDFSSinkConnectorConfig(
      hdfsURL = settings.stringValue(HDFS_URL),
      flushLineCount = settings.intOption(FLUSH_LINE_COUNT).orElse(FLUSH_LINE_COUNT_DEFAULT),
      rotateIntervalMS = settings.longOption(ROTATE_INTERVAL_MS).orElse(ROTATE_INTERVAL_MS_DEFAULT),
      tmpDir = tmpDir,
      dataDir = dataDir,
      dataFilePrefixName = prefixFileName,
      dataFileNeedHeader = settings.booleanOption(DATAFILE_NEEDHEADER).orElse(DATAFILE_NEEDHEADER_DEFAULT),
      dataBufferCount = settings.longOption(DATA_BUFFER_COUNT).orElse(DATA_BUFFER_COUNT_DEFAULT),
      hdfsStorageCreateClass =
        settings.stringOption(HDFS_STORAGE_CREATOR_CLASS).orElse(HDFS_STORAGE_CREATOR_CLASS_DEFAULT),
      dataFileEncode = settings.stringOption(DATAFILE_ENCODE).orElse(DATAFILE_ENCODE_DEFAULT)
    )
  }
}
