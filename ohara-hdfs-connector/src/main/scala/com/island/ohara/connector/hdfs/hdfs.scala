package com.island.ohara.connector

package object hdfs {
  val PREFIX_FILENAME_PATTERN: String = "[a-zA-Z0-9]*"
  val HDFS_URL: String = "hdfs.url"
  val FLUSH_LINE_COUNT: String = "flush.line.count"
  val ROTATE_INTERVAL_MS: String = "rotate.interval.ms"
  val TMP_DIR: String = "tmp.dir"
  val DATA_DIR: String = "data.dir"
  val DATAFILE_PREFIX_NAME: String = "datafile.prefix.name"
  val DATAFILE_NEEDHEADER: String = "datafile.isheader"
  val DATA_BUFFER_COUNT: String = "data.buffer.size"
  val HDFS_STORAGE_CREATOR_CLASS: String = "hdfs.storage.creator.class"
}
