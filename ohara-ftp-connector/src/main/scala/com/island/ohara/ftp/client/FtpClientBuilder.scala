package com.island.ohara.ftp.client

import java.io.{InputStream, OutputStream}
import java.util.Objects

import org.apache.commons.net.ftp.{FTP, FTPClient}

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

      override def delete(path: String): Unit = fileType(path) match {
        case FileType.FILE =>
          if (!client.deleteFile(path))
            throw new IllegalStateException(s"failed to delete $path because of ${client.getReplyCode}")
        case FileType.FOLDER      => throw new UnsupportedOperationException("unsupport to move folder")
        case FileType.NONEXISTENT => throw new IllegalStateException(s"$path doesn't exist")

      }

      override def tmpFolder: String = "tmp"
      override def exist(path: String): Boolean = {
        connectIfNeeded()
        client.getStatus(path) != null
      }
      override def fileType(path: String): FileType = if (exist(path)) {
        client.cwd(path) match {
          case 250 => FileType.FOLDER
          case _   => FileType.FILE
        }
      } else FileType.NONEXISTENT
    }
  }
}
