package com.island.ohara.client
import java.io.File
import java.nio.file.Paths

import akka.http.scaladsl.server.directives.FileInfo

trait StreamClient extends AutoCloseable {

  def saveFile(fileInfo: FileInfo): File

}

object StreamClient {

  /**
    * StreamApp List Page max acceptable file size (1 MB currently)
    */
  final val MAX_FILE_SIZE = 1 * 1024 * 1024L

  /**
    * StreamApp List Page "key name" for form-data
    */
  final val INPUT_KEY = "streamapp"

  final val TMP_ROOT = System.getProperty("java.io.tmpdir")

  /**
    * StreamApp List Page files save path (just for testing purpose for now...by sam)
    */
  final val JARS_ROOT = Paths.get(TMP_ROOT, "ohara_streams")

  /**
    * Save http request files by [[File.createTempFile]]
    *
    * @param fileInfo the request file
    * @return the tmp file
    */
  def saveTmpFile(fileInfo: FileInfo): File = File.createTempFile(fileInfo.fileName, ".tmp")
}
