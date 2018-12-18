package com.island.ohara.client
import java.io.File

import akka.http.scaladsl.server.directives.FileInfo

trait StreamClient extends AutoCloseable {

  def saveFile(fileInfo: FileInfo): File

}

object StreamClient {
  def saveTmpFile(fileInfo: FileInfo): File = File.createTempFile(fileInfo.fileName, ".tmp")
}
