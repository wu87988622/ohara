package com.island.ohara.ftp.client

import java.io._
import java.nio.charset.StandardCharsets
import java.nio.file.Files

import com.island.ohara.io.{CloseOnce, IoUtil}

/**
  * A general interface of ftp file system.
  */
trait FtpClient extends CloseOnce {
  def listFileNames(dir: String): Seq[String]

  def open(path: String): InputStream

  def create(path: String): OutputStream

  def moveFile(from: String, to: String): Unit

  def mkdir(path: String): Unit

  def delete(path: String): Unit

  def append(path: String, message: String): Unit = append(path, Seq(message))

  def append(path: String, messages: Seq[String]): Unit = {
    // add room to buffer data
    val size = messages.map(_.length).sum * 2
    CloseOnce.doClose(new BufferedWriter(new OutputStreamWriter(create(path), StandardCharsets.UTF_8), size)) {
      writer =>
        messages.foreach(line => {
          writer.append(line)
          writer.newLine()
        })
    }
  }

  def readLines(path: String): Seq[String] = readLines(path, (_) => true)

  def readLines(path: String, filter: String => Boolean): Seq[String] =
    IoUtil.readLines(new BufferedReader(new InputStreamReader(open(path), StandardCharsets.UTF_8)), filter)

  def upload(path: String, file: File): Unit = upload(path, Files.readAllBytes(file.toPath))

  def upload(path: String, data: Array[Byte]): Unit = {
    CloseOnce.doClose(create(path)) { output =>
      output.write(data, 0, data.length)
    }
  }

  def download(path: String): Array[Byte] = {
    CloseOnce.doClose(open(path)) { input =>
      {
        val output = new ByteArrayOutputStream()
        val buf = new Array[Byte](128)
        var rval: Int = Int.MaxValue
        while (rval > 0) {
          rval = input.read(buf)
          if (rval > 0) output.write(buf, 0, rval)
        }
        output.toByteArray
      }
    }
  }

  def tmpFolder: String

  def exist(path: String): Boolean

  def fileType(path: String): FileType
}

object FtpClient {
  def builder: FtpClientBuilder = new FtpClientBuilder
}

abstract sealed class FileType

object FileType {
  case object FILE extends FileType
  case object FOLDER extends FileType
  case object NONEXISTENT extends FileType
}
