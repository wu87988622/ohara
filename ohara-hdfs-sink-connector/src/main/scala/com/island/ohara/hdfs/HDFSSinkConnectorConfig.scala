package com.island.ohara.hdfs

import java.util

import org.apache.hadoop.conf.Configuration
import org.apache.kafka.common.config.ConfigDef
import org.apache.kafka.common.config.ConfigDef.Importance
import org.apache.kafka.common.config.ConfigDef.Type

/**
  * This class for props Map convert to object for convenient get data
  * @param props
  */
class HDFSSinkConnectorConfig(props: util.Map[String, String]) {
  if (props.get(HDFSSinkConnectorConfig.HDFS_URL) == null) {
    throw new RuntimeException(
      "Please assign the " +
        HDFSSinkConnectorConfig.HDFS_URL + " properties")
  }

  def hadoopConfiguration(): Configuration = {
    val config = new Configuration()
    config.set(HadoopConfigurationConstants.FS_DEFAULTFS, props.get(HDFSSinkConnectorConfig.HDFS_URL))
    config
  }

  def hdfsURL(): String = {
    props.get(HadoopConfigurationConstants.FS_DEFAULTFS)
  }

  def flushLineCount(): Int = {
    props
      .getOrDefault(HDFSSinkConnectorConfig.FLUSH_LINE_COUNT,
                    HDFSSinkConnectorConfig.FLUSH_LINE_COUNT_DEFAULT.toString())
      .toInt
  }

  def rotateIntervalMS(): Long = {
    props
      .getOrDefault(HDFSSinkConnectorConfig.ROTATE_INTERVAL_MS,
                    HDFSSinkConnectorConfig.ROTATE_INTERVAL_MS_DEFAULT.toString())
      .toLong
  }

  def tmpDir(): String = {
    props.getOrDefault(HDFSSinkConnectorConfig.TMP_DIR, HDFSSinkConnectorConfig.TMP_DIR_DEFAULT.toString())
  }

  def offsetDir(): String = {
    props.getOrDefault(HDFSSinkConnectorConfig.OFFSET_DIR, HDFSSinkConnectorConfig.OFFSET_DIR_DEFAULT)
  }

  def dataDir(): String = {
    props.getOrDefault(HDFSSinkConnectorConfig.DATA_DIR, HDFSSinkConnectorConfig.DATA_DIR_DEFAULT)
  }

  def dataFilePrefixName(): String = {
    props
      .getOrDefault(HDFSSinkConnectorConfig.DATAFILE_PREFIX_NAME, HDFSSinkConnectorConfig.DATAFILE_PREFIX_NAME_DEFAULT)
  }

  def offsetInconsistentSkip(): Boolean = {
    props
      .getOrDefault(HDFSSinkConnectorConfig.OFFSET_INCONSISTENT_SKIP,
                    HDFSSinkConnectorConfig.OFFSET_INCONSISTENT_SKIP_DEFAULT.toString())
      .toBoolean
  }

  def dataBufferCount(): Long = {
    props
      .getOrDefault(HDFSSinkConnectorConfig.DATA_BUFFER_COUNT,
                    HDFSSinkConnectorConfig.DATA_BUFFER_COUNT_DEFAULT.toString())
      .toLong
  }
}

object HDFSSinkConnectorConfig {
  val HDFS_URL: String = "hdfs.url"
  val FLUSH_LINE_COUNT: String = "flush.line.count"
  val FLUSH_LINE_COUNT_DEFAULT: Int = 1000
  val ROTATE_INTERVAL_MS: String = "rotate.interval.ms"
  val ROTATE_INTERVAL_MS_DEFAULT: Long = 60000
  val TMP_DIR: String = "tmp.dir"
  val TMP_DIR_DEFAULT: String = "/tmp"
  val OFFSET_DIR: String = "offset.dir"
  val OFFSET_DIR_DEFAULT: String = "/offset"
  val DATA_DIR: String = "data.dir"
  val DATA_DIR_DEFAULT = "/data"
  val DATAFILE_PREFIX_NAME: String = "datafile.prefix.name"
  val DATAFILE_PREFIX_NAME_DEFAULT: String = "part-"
  val OFFSET_INCONSISTENT_SKIP: String = "offset.inconsistent.skip"
  val OFFSET_INCONSISTENT_SKIP_DEFAULT: Boolean = false
  val DATA_BUFFER_COUNT: String = "data.buffer.size"
  val DATA_BUFFER_COUNT_DEFAULT: Long = 100
}
