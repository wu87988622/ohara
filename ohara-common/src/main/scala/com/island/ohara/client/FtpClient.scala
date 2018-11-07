package com.island.ohara.client

import java.io._
import java.nio.charset.{Charset, StandardCharsets}
import java.nio.file.Files
import java.util.Objects

import com.island.ohara.io.{CloseOnce, IoUtil}
import org.apache.commons.net.ftp.{FTP, FTPClient}
import scala.concurrent.duration._

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

  /**
    * create folder. It throw exception if there is already a folder
    * @param path folder path
    */
  def mkdir(path: String): Unit

  /**
    * recreate a folder. It will delete all stuff under the path.
    * @param path folder path
    */
  def reMkdir(path: String): Unit = {
    if (exist(path)) delete(path)
    mkdir(path)
  }

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
    val writer = new BufferedWriter(
      new OutputStreamWriter(if (exist(path)) append(path) else create(path), StandardCharsets.UTF_8),
      messages.map(_.length).sum * 2)
    try messages.foreach(line => {
      writer.append(line)
      writer.newLine()
    })
    finally writer.close()
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
  private[this] var hostname: String = _
  private[this] var port: Int = -1
  private[this] var user: String = _
  private[this] var password: String = _
  private[this] var retryCount = 1
  private[this] var retryInterval: Duration = 1 second
  def hostname(hostname: String): FtpClientBuilder = {
    this.hostname = hostname
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

  def disableRetry(): FtpClientBuilder = {
    this.retryCount = -1
    this
  }

  /**
    * set the retry count for the ftp client. Enabling the retry can reduce the failure when ftp connection is unstable.
    * @param retryCount retry limit
    * @return this builder
    */
  def retryCount(retryCount: Int): FtpClientBuilder = {
    this.retryCount = retryCount
    this
  }

  /**
    * the interval time before running retry.
    * @param retryInterval interval time
    * @return this builder
    */
  def retryInternal(retryInterval: Duration): FtpClientBuilder = {
    this.retryInterval = retryInterval
    this
  }

  def build(): FtpClient = {
    def check(s: String, msg: String): String =
      if (s == null || s.isEmpty) throw new IllegalArgumentException(msg) else s
    if (port < 0) throw new IllegalArgumentException("require port")
    if (retryInterval.toMillis < 0) throw new IllegalArgumentException(s"invalid retryInterval: $retryInterval")
    new FtpClient {
      private[this] val client = new FtpClientImpl(
        check(hostname, "hostname can't be null or empty"),
        port,
        check(user, "user can't be null or empty"),
        check(password, "password can't be null or empty")
      )
      private[this] def retry[T](function: () => T): T = IoUtil
        .retry(function = function, cleanup = () => close(), retryCount = retryCount, retryInterval = retryInterval)
      override def listFileNames(dir: String): Seq[String] = retry(() => client.listFileNames(dir))
      override def open(path: String): InputStream = retry(() => client.open(path))
      override def create(path: String): OutputStream = retry(() => client.create(path))
      override def append(path: String): OutputStream = retry(() => client.append(path))
      override def moveFile(from: String, to: String): Unit = retry(() => client.moveFile(from, to))
      override def mkdir(path: String): Unit = retry(() => client.mkdir(path))
      override def delete(path: String): Unit = retry(() => client.delete(path))
      override def tmpFolder: String = client.tmpFolder
      override def exist(path: String): Boolean = retry(() => client.exist(path))
      override def fileType(path: String): FileType = retry(() => client.fileType(path))
      override def status(): String = retry(() => client.status())
      override def workingFolder(): String = retry(() => client.workingFolder())
      override protected def doClose(): Unit = client.close()
    }
  }

  private[this] class FtpClientImpl(hostname: String, port: Int, user: String, password: String) extends FtpClient {
    private[this] var _client: FTPClient = _

    private[this] def connectIfNeeded(): FTPClient = if (connected) _client
    else {
      if (_client == null) _client = new FTPClient
      _client.connect(hostname, port)
      _client.enterLocalPassiveMode()
      _client.login(user, password)
      _client
    }

    private[this] def connected: Boolean = _client != null && _client.isConnected

    override def listFileNames(dir: String): Seq[String] = connectIfNeeded().listFiles(dir).map(_.getName).toSeq

    override def open(path: String): InputStream = {
      val client = connectIfNeeded()
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
          if (client == null || !client.completePendingCommand())
            throw new IllegalStateException("Failed to complete pending command")
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
      val client = connectIfNeeded()
      if (!client.changeWorkingDirectory(path) && !client.makeDirectory(path))
        throw new IllegalStateException(s"Failed to create folder on $path")
    }

    override protected def doClose(): Unit = if (connected) {
      _client.logout()
      _client.disconnect()
      _client = null
    }

    override def create(path: String): OutputStream = {
      val client = connectIfNeeded()
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
      val client = connectIfNeeded()
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
        val client = connectIfNeeded()
        if (!client.deleteFile(path))
          throw new IllegalStateException(s"failed to delete $path because of ${client.getReplyCode}")
      case FileType.FOLDER =>
        val clinet = connectIfNeeded()
        if (!clinet.removeDirectory(path))
          throw new IllegalStateException(s"failed to delete $path because of ${clinet.getReplyCode}")
      case FileType.NONEXISTENT => throw new IllegalStateException(s"$path doesn't exist")

    }

    override def tmpFolder: String = "/tmp"
    override def exist(path: String): Boolean = {
      val client = connectIfNeeded()
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
      val client = connectIfNeeded()
      val current = client.printWorkingDirectory()
      try client.cwd(path) match {
        case 250 => FileType.FOLDER
        case _   => FileType.FILE
      } finally client.cwd(current)
    } else FileType.NONEXISTENT

    override def status(): String = connectIfNeeded().getStatus

    override def workingFolder(): String = connectIfNeeded().printWorkingDirectory()
  }
}
