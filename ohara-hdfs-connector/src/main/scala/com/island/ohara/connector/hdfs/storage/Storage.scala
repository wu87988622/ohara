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

package com.island.ohara.connector.hdfs.storage

import java.io.OutputStream

import com.island.ohara.common.util.Releasable

abstract class Storage extends Releasable {

  /**
    * List children folder path from path parameter
    * @param path
    */
  def list(path: String): Iterator[String]

  /**
    * Data append to file
    *
    * The OutputStream returned by this method should be closed manually.
    * Storage won't manage the stream for users.
    *
    * @param filePathAndName
    * @return
    */
  def append(filePathAndName: String): OutputStream

  /**
    * Create the file OutputStream
    *
    * The OutputStream returned by this method should be closed manually.
    * Storage won't manage the stream for users.
    *
    * @param filePathAndName
    * @return
    */
  def open(filePathAndName: String, overwrite: Boolean): OutputStream

  /**
    * Create folder
    * @param folderPathAndName
    * @return
    */
  def mkdirs(folderPathAndName: String): Boolean

  /**
    * Check the folder or file, is it exists
    * @param path
    * @return
    */
  def exists(path: String): Boolean

  /**
    * Delete the file or folder
    * @param filePathAndName
    */
  def delete(filePathAndName: String, recursive: Boolean): Boolean

  /**
    * file name rename from source path to target path
    * @param sourcePath
    * @param targetPath
    */
  def renameFile(sourcePath: String, targetPath: String): Boolean
}
