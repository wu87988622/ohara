package com.island.ohara.hdfs.creator

import com.island.ohara.hdfs.storage.Storage

/**
  * This abstract for define the storage instance
  */
abstract class StorageCreator {

  /**
    * get storage instance
    * @return
    */
  def getStorage(): Storage

  /**
    * close storage filesystem
    */
  def close(): Unit
}
