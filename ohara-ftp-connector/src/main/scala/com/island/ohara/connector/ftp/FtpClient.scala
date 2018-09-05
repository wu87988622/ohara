package com.island.ohara.connector.ftp

import java.io._
import java.nio.charset.{Charset, StandardCharsets}
import java.nio.file.Files

import com.island.ohara.io.CloseOnce

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

  /**
    * read all content from the path.
    * @param path file path
    * @param encode encode (UTF-8 is default)
    * @return an array of lines
    */
  def readLines(path: String, encode: String = "UTF-8"): Array[String] =
    CloseOnce.doClose(new BufferedReader(new InputStreamReader(open(path), Charset.forName(encode)))) { reader =>
      Iterator.continually(reader.readLine()).takeWhile(_ != null).toArray
    }

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
