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
