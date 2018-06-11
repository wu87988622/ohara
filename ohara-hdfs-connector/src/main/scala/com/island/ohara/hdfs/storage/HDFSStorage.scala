package com.island.ohara.hdfs.storage

import java.io.OutputStream
import java.net.URI
import com.island.ohara.hdfs.HDFSSinkConnectorConfig
import com.typesafe.scalalogging.Logger
import org.apache.hadoop.fs.{FileSystem, Path, RemoteIterator}

class HDFSStorage(fileSystem: FileSystem) extends Storage {
  private[this] lazy val logger = Logger(getClass().getName())

  /**
    * List children folder or file path from path parameter
    *
    * @param path
    */
  override def list(path: String): Iterator[String] = {
    implicit def convertToScalaIterator[T](underlying: RemoteIterator[T]): Iterator[T] = {
      case class wrapper(underlying: RemoteIterator[T]) extends Iterator[T] {
        override def hasNext: Boolean = underlying.hasNext()

        override def next(): T = underlying.next()
      }
      wrapper(underlying)
    }

    val hdfsPath = new Path(path)
    if (fileSystem.exists(hdfsPath))
      fileSystem
        .listLocatedStatus(hdfsPath)
        .map(fileStatus => {
          fileStatus.getPath().toString()
        })
    else Iterator.empty
  }

  /**
    * Data append to file
    *
    * The OutputStream returned by this method should be closed manually.
    * Storage won't manage the stream for users.
    *
    * @param filePathAndName
    * @return
    */
  override def append(filePathAndName: String): OutputStream = {
    fileSystem.append(new Path(filePathAndName))
  }

  /**
    * Create the file OutputStream
    *
    * The OutputStream returned by this method should be closed manually.
    * Storage won't manage the stream for users.
    *
    * @param filePathAndName
    * @return
    */
  override def open(filePathAndName: String, overwrite: Boolean): OutputStream = {
    fileSystem.create(new Path(filePathAndName), overwrite)
  }

  /**
    * Create folder
    *
    * @param folderPathAndName
    * @return
    */
  override def mkdirs(folderPathAndName: String): Boolean = {
    fileSystem.mkdirs(new Path(folderPathAndName))
  }

  /**
    * Check the folder or file, is it exists
    *
    * @param path
    * @return
    */
  override def exists(path: String): Boolean = {
    fileSystem.exists(new Path(path))
  }

  /**
    * Delete the file or folder
    *
    * @param filePathAndName
    */
  override def delete(filePathAndName: String, recursive: Boolean): Boolean = {
    fileSystem.delete(new Path(filePathAndName), recursive)
  }

  /**
    * file name rename from source path to target path
    *
    * @param sourcePath
    * @param targetPath
    */
  override def renameFile(sourcePath: String, targetPath: String): Boolean = {
    if (exists(targetPath)) {
      val errorMessage = s"The target path: ${targetPath} is exists"
      throw new RuntimeException(errorMessage)
    }

    if (sourcePath.equals(targetPath)) {
      logger.error("The source path equals the target path")
      return false
    }

    val srcPath = new Path(sourcePath)
    val dstPath = new Path(targetPath)

    if (exists(sourcePath)) {
      fileSystem.rename(srcPath, dstPath)
    } else {
      val errorMessage = s"The source path: ${sourcePath} is exists"
      logger.error(errorMessage)
      throw new RuntimeException(errorMessage)
    }
  }

  /**
    * close file OutputStream
    */
  override def close(): Unit = {
    fileSystem.close()
  }
}
