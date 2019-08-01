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

package com.island.ohara.connector.hdfs.sink

import java.io.{InputStream, OutputStream}
import java.util
import java.util.Collections

import com.island.ohara.kafka.connector.storage.Storage
import com.typesafe.scalalogging.Logger
import org.apache.hadoop.fs.{FileSystem, Path, PathFilter, RemoteIterator}

import scala.collection.JavaConverters._

class HDFSStorage(fileSystem: FileSystem) extends Storage {
  private[this] lazy val logger = Logger(getClass.getName)

  override def exists(path: String): Boolean = {
    fileSystem.exists(new Path(path))
  }

  override def list(path: String): util.Iterator[String] = {
    implicit def convertToScalaIterator[T](underlying: RemoteIterator[T]): Iterator[T] = {
      case class wrapper(underlying: RemoteIterator[T]) extends Iterator[T] {
        override def hasNext: Boolean = underlying.hasNext

        override def next(): T = underlying.next()
      }
      wrapper(underlying)
    }

    if (exists(path))
      fileSystem
        .listLocatedStatus(new Path(path))
        .map(fileStatus => {
          fileStatus.getPath.toString
        })
        .asJava
    else Collections.emptyIterator()
  }

  def list(path: String, pathFilter: PathFilter): util.List[String] = {
    if (exists(path)) {
      fileSystem.listStatus(new Path(path), pathFilter).map(fileStat => fileStat.getPath.toString).toList.asJava
    } else {
      Collections.emptyList()
    }
  }

  override def create(path: String, overwrite: Boolean): OutputStream = {
    fileSystem.create(new Path(path), overwrite)
  }

  override def append(path: String): OutputStream = {
    fileSystem.append(new Path(path))
  }

  override def open(path: String): InputStream = {
    fileSystem.open(new Path(path))
  }

  override def delete(path: String): Unit = {
    fileSystem.delete(new Path(path), true)
  }

  override def move(sourcePath: String, targetPath: String): Boolean = {
    if (exists(targetPath)) {
      val errorMessage = s"The target path: $targetPath is exists"
      throw new RuntimeException(errorMessage)
    }

    if (sourcePath == targetPath) {
      logger.error("The source path equals the target path")
      return false
    }

    val srcPath = new Path(sourcePath)
    val dstPath = new Path(targetPath)

    if (exists(sourcePath)) {
      fileSystem.rename(srcPath, dstPath)
    } else {
      val errorMessage = s"The source path: $sourcePath is not exists"
      logger.error(errorMessage)
      throw new RuntimeException(errorMessage)
    }
  }

  override def close(): Unit = {
    fileSystem.close()
  }
}
