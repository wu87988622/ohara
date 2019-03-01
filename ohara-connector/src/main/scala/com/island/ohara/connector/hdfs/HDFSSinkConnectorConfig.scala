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
  private[this] val FLUSH_LINE_COUNT_DEFAULT: Int = 1000
  private[this] val ROTATE_INTERVAL_MS_DEFAULT: Long = 60000
  private[this] val TMP_DIR_DEFAULT: String = "/tmp"
  private[connector] val DATA_DIR_DEFAULT = "/data"
  private[this] val DATAFILE_NEEDHEADER_DEFAULT: Boolean = true
  private[this] val DATAFILE_PREFIX_NAME_DEFAULT: String = "part"
  private[this] val DATA_BUFFER_COUNT_DEFAULT: Long = 100
  private[this] val HDFS_STORAGE_CREATOR_CLASS_DEFAULT: String = classOf[HDFSStorageCreator].getName
  private[this] val DATAFILE_ENCODE_DEFAULT = "UTF-8"

  def apply(props: Map[String, String]): HDFSSinkConnectorConfig = {
    val prefixFileName: String = props.getOrElse(DATAFILE_PREFIX_NAME, DATAFILE_PREFIX_NAME_DEFAULT)
    if (!prefixFileName.matches(PREFIX_FILENAME_PATTERN)) {
      throw new IllegalArgumentException("The " + DATAFILE_PREFIX_NAME + " value only a-z or A-Z or 0-9")
    }

    val tmpDir: String = props.getOrElse(TMP_DIR, TMP_DIR_DEFAULT)
    val dataDir: String = props.getOrElse(DATA_DIR, DATA_DIR_DEFAULT)
    if (tmpDir.equals(dataDir)) {
      throw new IllegalArgumentException("The tmpDir path same as dataDir path, Please input different path.")
    }

    HDFSSinkConnectorConfig(
      hdfsURL = props(HDFS_URL),
      flushLineCount = props.getOrElse(FLUSH_LINE_COUNT, FLUSH_LINE_COUNT_DEFAULT.toString).toInt,
      rotateIntervalMS = props.getOrElse(ROTATE_INTERVAL_MS, ROTATE_INTERVAL_MS_DEFAULT.toString).toLong,
      tmpDir = tmpDir,
      dataDir = dataDir,
      dataFilePrefixName = prefixFileName,
      dataFileNeedHeader = props.getOrElse(DATAFILE_NEEDHEADER, DATAFILE_NEEDHEADER_DEFAULT.toString).toBoolean,
      dataBufferCount = props.getOrElse(DATA_BUFFER_COUNT, DATA_BUFFER_COUNT_DEFAULT.toString).toLong,
      hdfsStorageCreateClass = props.getOrElse(HDFS_STORAGE_CREATOR_CLASS, HDFS_STORAGE_CREATOR_CLASS_DEFAULT),
      dataFileEncode = props.getOrElse(DATAFILE_ENCODE, DATAFILE_ENCODE_DEFAULT)
    )
  }
}
