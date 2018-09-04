package com.island.ohara.connector.hdfs.creator

import com.island.ohara.connector.hdfs.storage.Storage

/**
  * This abstract for define the storage instance
  */
abstract class StorageCreator {

  /**
    * get storage instance
    * @return
    */
  def storage(): Storage

  /**
    * close storage filesystem
    */
  def close(): Unit
}
