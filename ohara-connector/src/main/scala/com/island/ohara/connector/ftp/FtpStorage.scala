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

package com.island.ohara.connector.ftp

import java.io.{InputStream, OutputStream}
import java.nio.file.{Path, Paths}
import java.util

import com.island.ohara.client.ftp.{FileType, FtpClient}
import com.island.ohara.common.annotations.VisibleForTesting
import com.island.ohara.common.exception.{OharaException, OharaFileAlreadyExistsException}
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.kafka.connector.storage.Storage
import com.typesafe.scalalogging.Logger

import scala.collection.JavaConverters._

class FtpStorage(ftpClient: FtpClient) extends Storage {
  private[this] lazy val LOG = Logger(getClass.getName)

  /**
    * Returns whether a file or folder exists.
    *
    * @param path the path to the file or folder.
    * @return true if file or folder exists, false otherwise.
    */
  override def exists(path: String): Boolean = ftpClient.exist(path)

  /**
    * List the path of files at a given folder path.
    *
    * @param path the folder path.
    * @throws OharaException if the folder does not exist.
    * @return the path listing of the files or folders.
    */
  override def list(path: String): util.Iterator[Path] = if (exists(path))
    ftpClient.listFileNames(path).map(name => Paths.get(path, name)).iterator.asJava
  else throw new OharaException(s"${path} doesn't exist")

  /**
    * Creates a new file in the given path.
    *
    * @param path the path of the file to be created.
    * @throws OharaFileAlreadyExistsException if a file of that path already exists.
    * @throws OharaException if the parent folder does not exist.
    * @return an output stream associated with the new file.
    */
  override def create(path: String): OutputStream = {
    if (exists(path)) {
      throw new OharaFileAlreadyExistsException(s"File already exists: ${path}")
    }

    try {
      val parent = Paths.get(path).getParent.toString
      if (!exists(parent)) mkdirs(parent)
      ftpClient.create(path)
    } catch {
      case e: Throwable => throw new OharaException(e)
    }
  }

  /**
    * Append data to an existing file at the given path (optional operation).
    *
    * @param path the path of the file to be appended.
    * @throws OharaException if the file path does not exist.
    * @return an output stream associated with the existing file.
    */
  override def append(path: String): OutputStream =
    if (exists(path)) ftpClient.append(path) else throw new OharaException(s"${path} doesn't exist")

  /**
    * Open for reading an file at the given path.
    *
    * @param path the path of the file to be read.
    * @return an input stream with the requested file.
    */
  override def open(path: String): InputStream = ftpClient.open(path)

  /**
    * Delete the given file or folder.
    *
    * <p>NOTED: Do nothing if the path does not exist.
    *
    * @param path path the path to the file or folder to delete.
    */
  override def delete(path: String): Unit = if (exists(path)) ftpClient.delete(path)

  @VisibleForTesting
  private[ftp] def delete(path: String, recursive: Boolean): Unit = {
    if (exists(path)) {
      if (recursive) {
        if (isFolder(path)) {
          ftpClient
            .listFileNames(path)
            .map(fileName => {
              val child = CommonUtils.path(path, fileName)
              delete(child, recursive)
            })
        }
        delete(path)
      } else delete(path)
    }
  }

  private[this] def isFolder(path: String): Boolean = ftpClient.fileType(path) == FileType.FOLDER

  /**
    * Move or rename a file from source path to target path.
    *
    * @param sourcePath the path to the file to move
    * @param targetPath the path to the target file
    * @return true if file have moved to target path , false otherwise.
    */
  override def move(sourcePath: String, targetPath: String): Boolean = {
    if (!exists(sourcePath)) {
      val errorMessage = s"The source path: $sourcePath is not exists"
      throw new RuntimeException(errorMessage)
    }
    if (exists(targetPath)) {
      val errorMessage = s"The target path: $targetPath is exists"
      throw new RuntimeException(errorMessage)
    }
    if (sourcePath == targetPath) {
      LOG.error("The source path equals the target path")
      return false
    }

    ftpClient.moveFile(sourcePath, targetPath)
    exists(targetPath)
  }

  /**
    * creates folder, including any necessary but nonexistent parent folders
    *
    * @param path folder path
    */
  override def mkdirs(path: String): Unit = {
    var folders = Seq[String](path)

    var parent = Paths.get(path).getParent
    while (!exists(parent.toString)) {
      folders = parent.toString +: folders
      parent = parent.getParent
    }

    folders.map(folder => mkdir(folder))
  }

  private[this] def mkdir(path: String): Unit = if (!exists(path)) ftpClient.mkdir(path)

  /** Stop using this storage. */
  override def close(): Unit = Releasable.close(ftpClient)
}
