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

package com.island.ohara.connector.hdfs.creator

import java.net.URI

import com.island.ohara.common.util.ReleaseOnce
import com.island.ohara.connector.hdfs.HDFSSinkConnectorConfig
import com.island.ohara.connector.hdfs.storage.{HDFSStorage, Storage}
import org.apache.hadoop.fs.FileSystem

/**
  * This class is creating HDFSStorage instance
  * @param config
  */
class HDFSStorageCreator(config: HDFSSinkConnectorConfig) extends StorageCreator {
  private[this] val hdfsURL: String = config.hdfsURL
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
    ReleaseOnce.close(fileSystem)
  }
}
