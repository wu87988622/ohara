package com.island.ohara.connector.hdfs

import com.island.ohara.connector.hdfs.creator.HDFSStorageCreator
import org.apache.hadoop.conf.Configuration

/**
  * This class for props Map convert to object for convenient get data
  * @param props
  */
class HDFSSinkConnectorConfig(props: Map[String, String]) {
  val PREFIX_FILENAME_PATTERN: String = "[a-zA-Z0-9]*"

  if (props.get(HDFSSinkConnectorConfig.HDFS_URL) == null) {
    throw new RuntimeException(
      "Please assign the " +
        HDFSSinkConnectorConfig.HDFS_URL + " properties")
  }

  def hadoopConfiguration(): Configuration = {
    val config = new Configuration()
    config.set(HadoopConfigurationConstants.FS_DEFAULTFS, hdfsURL())
    config
  }

  def hdfsURL(): String = props(HDFSSinkConnectorConfig.HDFS_URL)

  def flushLineCount(): Int = {
    props
      .getOrElse(HDFSSinkConnectorConfig.FLUSH_LINE_COUNT, HDFSSinkConnectorConfig.FLUSH_LINE_COUNT_DEFAULT.toString)
      .toInt
  }

  def rotateIntervalMS(): Long = {
    props
      .getOrElse(HDFSSinkConnectorConfig.ROTATE_INTERVAL_MS,
                 HDFSSinkConnectorConfig.ROTATE_INTERVAL_MS_DEFAULT.toString)
      .toLong
  }

  def tmpDir(): String = {
    props.getOrElse(HDFSSinkConnectorConfig.TMP_DIR, HDFSSinkConnectorConfig.TMP_DIR_DEFAULT.toString)
  }

  def offsetDir(): String = {
    props.getOrElse(HDFSSinkConnectorConfig.OFFSET_DIR, HDFSSinkConnectorConfig.OFFSET_DIR_DEFAULT)
  }

  def dataDir(): String = {
    props.getOrElse(HDFSSinkConnectorConfig.DATA_DIR, HDFSSinkConnectorConfig.DATA_DIR_DEFAULT)
  }

  def dataFilePrefixName(): String = {
    val prefixFileName: String = props
      .getOrElse(HDFSSinkConnectorConfig.DATAFILE_PREFIX_NAME, HDFSSinkConnectorConfig.DATAFILE_PREFIX_NAME_DEFAULT)

    if (!prefixFileName.matches(PREFIX_FILENAME_PATTERN)) {
      throw new RuntimeException(
        "The " + HDFSSinkConnectorConfig.DATAFILE_PREFIX_NAME + " value only a-z or A-Z or 0-9")
    }
    prefixFileName
  }

  def offsetInconsistentSkip(): Boolean = {
    props
      .getOrElse(HDFSSinkConnectorConfig.OFFSET_INCONSISTENT_SKIP,
                 HDFSSinkConnectorConfig.OFFSET_INCONSISTENT_SKIP_DEFAULT.toString)
      .toBoolean
  }

  def dataBufferCount(): Long = {
    props
      .getOrElse(HDFSSinkConnectorConfig.DATA_BUFFER_COUNT, HDFSSinkConnectorConfig.DATA_BUFFER_COUNT_DEFAULT.toString)
      .toLong
  }

  def hdfsStorageCreatorClass(): String = {
    props.getOrElse(HDFSSinkConnectorConfig.HDFS_STORAGE_CREATOR_CLASS,
                    HDFSSinkConnectorConfig.HDFS_STORAGE_CREATOR_CLASS_DEFAULT)
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
  val DATAFILE_PREFIX_NAME_DEFAULT: String = "part"
  val OFFSET_INCONSISTENT_SKIP: String = "offset.inconsistent.skip"
  val OFFSET_INCONSISTENT_SKIP_DEFAULT: Boolean = false
  val DATA_BUFFER_COUNT: String = "data.buffer.size"
  val DATA_BUFFER_COUNT_DEFAULT: Long = 100
  val HDFS_STORAGE_CREATOR_CLASS: String = "hdfs.storage.creator.class"
  val HDFS_STORAGE_CREATOR_CLASS_DEFAULT: String = classOf[HDFSStorageCreator].getName()
}
