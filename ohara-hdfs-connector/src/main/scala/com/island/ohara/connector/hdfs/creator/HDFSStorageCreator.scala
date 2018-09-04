package com.island.ohara.connector.hdfs.creator

import java.net.URI

import com.island.ohara.connector.hdfs.HDFSSinkConnectorConfig
import com.island.ohara.connector.hdfs.storage.{HDFSStorage, Storage}
import com.island.ohara.io.CloseOnce
import org.apache.hadoop.fs.FileSystem

/**
  * This class is creating HDFSStorage instance
  * @param config
  */
class HDFSStorageCreator(config: HDFSSinkConnectorConfig) extends StorageCreator {
  private[this] val hdfsURL: String = config.hdfsURL()
  private[this] val fileSystem: FileSystem = FileSystem.newInstance(URI.create(hdfsURL), config.hadoopConfiguration())
  private[this] val hdfsStorage: HDFSStorage = new HDFSStorage(fileSystem)

  /**
    * Get HDFSStorage instance
    * @return
    */
  override def storage(): Storage = {
    hdfsStorage
  }

  /**
    * Close HDFS FileSystem
    */
  override def close(): Unit = {
    CloseOnce.close(fileSystem)
  }
}
