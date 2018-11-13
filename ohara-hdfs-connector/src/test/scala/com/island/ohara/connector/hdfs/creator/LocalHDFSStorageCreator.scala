package com.island.ohara.connector.hdfs.creator

import com.island.ohara.connector.hdfs.HDFSSinkConnectorConfig
import com.island.ohara.connector.hdfs.storage.{HDFSStorage, Storage}
import com.island.ohara.integration.OharaTestUtil
import com.island.ohara.client.util.CloseOnce
import org.apache.hadoop.fs.FileSystem

class LocalHDFSStorageCreator(config: HDFSSinkConnectorConfig) extends StorageCreator {
  private[this] val fileSystem: FileSystem = OharaTestUtil.localHDFS().hdfs.fileSystem
  private[this] val hdfsStorage: HDFSStorage = new HDFSStorage(fileSystem)

  override def storage(): Storage = {
    hdfsStorage
  }

  override def close(): Unit = {
    CloseOnce.close(fileSystem)
  }
}
