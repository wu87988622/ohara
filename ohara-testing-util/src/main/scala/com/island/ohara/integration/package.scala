package com.island.ohara

import java.io.{File, FileNotFoundException}
import java.net.ServerSocket

import scala.util.Random

package object integration {
  def availablePort: Int = {
    val socket = new ServerSocket(0)
    try socket.getLocalPort
    finally socket.close()
  }

  def createTempDir(dirPrefix: String): File = {
    var count: Int = 50
    while (count >= 0) {
      val file = new File(System.getProperty("java.io.tmpdir"), dirPrefix + Random.nextInt(100000))
      if (!file.exists()) {
        if (file.mkdirs) return file
        else throw new RuntimeException("could not create temp directory: " + file.getAbsolutePath)
      }
      count -= 1
    }
    throw new IllegalStateException("Failed to create tmp folder")
  }

  def deleteFile(path: File): Boolean = {
    if (!path.exists) throw new FileNotFoundException(path.getAbsolutePath)
    var ret = true
    if (path.isDirectory) {
      for (f <- path.listFiles) {
        ret = ret && deleteFile(f)
      }
    }
    ret && path.delete
  }
}
