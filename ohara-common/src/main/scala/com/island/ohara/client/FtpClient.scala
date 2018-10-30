package com.island.ohara.client

import java.io._
import java.nio.charset.{Charset, StandardCharsets}
import java.nio.file.Files
import java.util.Objects

import com.island.ohara.io.{CloseOnce, IoUtil}
import org.apache.commons.net.ftp.{FTP, FTPClient}

/**
  * A general interface of ftp file system.
  */
trait FtpClient extends CloseOnce {
  def listFileNames(dir: String): Seq[String]

  /**
    * open a input stream from a existent file. If file doesn't exist, an IllegalArgumentException will be thrown.
    * @param path file path
    * @return input stream
    */
  def open(path: String): InputStream

  /**
    * create an new file. If file already exists, an IllegalArgumentException will be thrown.
    * @param path file path
    * @return file output stream
    */
  def create(path: String): OutputStream

  /**
    * create output stream from an existent file. If file doesn't exist, an IllegalArgumentException will be thrown.
    * @param path file path
    * @return file output stream
    */
  def append(path: String): OutputStream

  def moveFile(from: String, to: String): Unit

  def mkdir(path: String): Unit

  def delete(path: String): Unit

  /**
    * append message to the end of file. If the file doesn't exist, it will create an new file.
    * @param path file path
    * @param message message
    */
  def attach(path: String, message: String): Unit = attach(path, Seq(message))

  /**
    * append messages to the end of file. If the file doesn't exist, it will create an new file.
    * @param path file path
    * @param messages messages
    */
  def attach(path: String, messages: Seq[String]): Unit = {
    // add room to buffer data
    val size = messages.map(_.length).sum * 2
    CloseOnce.doClose2(new OutputStreamWriter(if (exist(path)) append(path) else create(path), StandardCharsets.UTF_8))(
      new BufferedWriter(_, size)) {
      case (_, writer) =>
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
    CloseOnce.doClose2(open(path))(_ => new ByteArrayOutputStream()) { (input, output) =>
      val buf = new Array[Byte](128)
      Iterator.continually(input.read(buf)).takeWhile(_ > 0).foreach(output.write(buf, 0, _))
      output.toByteArray
    }
  }

  def tmpFolder: String

  def exist(path: String): Boolean

  def nonExist(path: String): Boolean = !exist(path)

  def fileType(path: String): FileType

  def status(): String

  def workingFolder(): String
}

object FtpClient {
  def builder(): FtpClientBuilder = new FtpClientBuilder
}

abstract sealed class FileType

object FileType {
  case object FILE extends FileType
  case object FOLDER extends FileType
  case object NONEXISTENT extends FileType
}

class FtpClientBuilder {
  private[this] var host: String = _
  private[this] var port: Int = -1
  private[this] var user: String = _
  private[this] var password: String = _

  def host(host: String): FtpClientBuilder = {
    this.host = host
    this
  }

  def port(port: Int): FtpClientBuilder = {
    this.port = port
    this
  }

  def user(user: String): FtpClientBuilder = {
    this.user = user
    this
  }

  def password(password: String): FtpClientBuilder = {
    this.password = password
    this
  }

  def build(): FtpClient = {
    Objects.requireNonNull(host)
    Objects.requireNonNull(user)
    Objects.requireNonNull(password)
    if (port < 0) throw new IllegalArgumentException("require port")
    new FtpClient {
      private[this] val client = new FTPClient

      private[this] def connectIfNeeded(): FTPClient = if (!connected) {
        client.connect(host, port)
        client.enterLocalPassiveMode()
        client.login(user, password)
        client
      } else client

      private[this] def connected: Boolean = client.isConnected

      override def listFileNames(dir: String): Seq[String] = connectIfNeeded().listFiles(dir).map(_.getName).toSeq

      override def open(path: String): InputStream = {
        connectIfNeeded()
        client.setFileType(FTP.BINARY_FILE_TYPE)
        if (nonExist(path)) throw new IllegalArgumentException(s"$path doesn't exist")
        val inputStream = client.retrieveFileStream(path)
        if (inputStream == null)
          throw new IllegalStateException(s"Failed to open $path because of ${client.getReplyCode}")
        new InputStream {
          override def read(): Int = inputStream.read()

          override def available(): Int = inputStream.available()

          override def close(): Unit = {
            inputStream.close()
            if (!client.completePendingCommand()) throw new IllegalStateException("Failed to complete pending command")
          }

          override def mark(readlimit: Int): Unit = inputStream.mark(readlimit)

          override def markSupported(): Boolean = inputStream.markSupported()

          override def read(b: Array[Byte]): Int = inputStream.read(b)

          override def read(b: Array[Byte], off: Int, len: Int): Int = inputStream.read(b, off, len)

          override def reset(): Unit = inputStream.reset()

          override def skip(n: Long): Long = inputStream.skip(n)

          override def equals(obj: scala.Any): Boolean = inputStream.equals(obj)

          override def hashCode(): Int = inputStream.hashCode()

          override def toString: String = inputStream.toString
        }
      }

      override def moveFile(from: String, to: String): Unit = if (!connectIfNeeded().rename(from, to))
        throw new IllegalStateException(s"Failed to move file from $from to $to")

      override def mkdir(path: String): Unit = {
        connectIfNeeded()
        if (!client.changeWorkingDirectory(path) && !client.makeDirectory(path))
          throw new IllegalStateException(s"Failed to create folder on $path")
      }

      override protected def doClose(): Unit = if (connected) {
        client.logout()
        client.disconnect()
      }

      override def create(path: String): OutputStream = {
        connectIfNeeded()
        client.setFileType(FTP.BINARY_FILE_TYPE)
        if (exist(path)) throw new IllegalArgumentException(s"$path exists")
        val outputStream = client.storeFileStream(path)
        if (outputStream == null)
          throw new IllegalStateException(s"Failed to create $path because of ${client.getReplyCode}")
        new OutputStream {
          override def write(b: Int): Unit = outputStream.write(b)

          override def close(): Unit = {
            outputStream.close()
            if (!client.completePendingCommand()) throw new IllegalStateException("Failed to complete pending command")
          }

          override def flush(): Unit = outputStream.flush()

          override def write(b: Array[Byte]): Unit = outputStream.write(b)

          override def write(b: Array[Byte], off: Int, len: Int): Unit = outputStream.write(b, off, len)

          override def equals(obj: scala.Any): Boolean = outputStream.equals(obj)

          override def hashCode(): Int = outputStream.hashCode()

          override def toString: String = outputStream.toString
        }
      }

      override def append(path: String): OutputStream = {
        connectIfNeeded()
        client.setFileType(FTP.BINARY_FILE_TYPE)
        if (nonExist(path)) throw new IllegalArgumentException(s"$path doesn't exist")
        val outputStream = client.appendFileStream(path)
        if (outputStream == null)
          throw new IllegalStateException(s"Failed to create $path because of ${client.getReplyCode}")
        new OutputStream {
          override def write(b: Int): Unit = outputStream.write(b)

          override def close(): Unit = {
            outputStream.close()
            if (!client.completePendingCommand()) throw new IllegalStateException("Failed to complete pending command")
          }

          override def flush(): Unit = outputStream.flush()

          override def write(b: Array[Byte]): Unit = outputStream.write(b)

          override def write(b: Array[Byte], off: Int, len: Int): Unit = outputStream.write(b, off, len)

          override def equals(obj: scala.Any): Boolean = outputStream.equals(obj)

          override def hashCode(): Int = outputStream.hashCode()

          override def toString: String = outputStream.toString
        }
      }

      override def delete(path: String): Unit = fileType(path) match {
        case FileType.FILE =>
          if (!client.deleteFile(path))
            throw new IllegalStateException(s"failed to delete $path because of ${client.getReplyCode}")
        case FileType.FOLDER =>
          if (!client.removeDirectory(path))
            throw new IllegalStateException(s"failed to delete $path because of ${client.getReplyCode}")
        case FileType.NONEXISTENT => throw new IllegalStateException(s"$path doesn't exist")

      }

      override def tmpFolder: String = "/tmp"
      override def exist(path: String): Boolean = {
        connectIfNeeded()
        val result = client.getStatus(path)
        // different ftp implementations have different return value...
        if (result == null) false
        else {
          // if path references to folder, some ftp servers return "212-"
          if (result.startsWith("212-")) true
          else
            result.contains(IoUtil.name(path)) || // if path references to file, result will show the meta of files
            result.contains(IoUtil.name("..")) // if path references to folder, result will show meta of all files with "." and "..
        }
      }

      override def fileType(path: String): FileType = if (exist(path)) {
        val current = client.printWorkingDirectory()
        try client.cwd(path) match {
          case 250 => FileType.FOLDER
          case _   => FileType.FILE
        } finally client.cwd(current)
      } else FileType.NONEXISTENT

      override def status(): String = {
        connectIfNeeded()
        client.getStatus
      }

      override def workingFolder(): String = {
        connectIfNeeded()
        client.printWorkingDirectory()
      }
    }
  }
}
