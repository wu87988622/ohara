package com.island.ohara.integration

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import com.island.ohara.io.CloseOnce
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}

/**
  * HDFS client
  * Using external HDFS or local FileSystem
  */
trait Hdfs extends CloseOnce {

  val hdfsURL: String
  val tmpDirectory: String
  val isLocal: Boolean

  def fileSystem(): FileSystem
}

object Hdfs {
  private[integration] val HDFS: String = "ohara.it.hdfs"

  private[integration] def apply(): Hdfs = {
    this.apply(sys.env.get(HDFS))
  }

  private[integration] def apply(hdfs: Option[String]): Hdfs = {
    var tmpFile: Option[File] = None

    var _hdfsUrl: String = ""
    var _tmpDirectory: String = ""
    var _isLocal: Boolean = true

    hdfs
      .map(_.toLowerCase())
      .map(x => {
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        val timeString = now.format(formatter)
        _tmpDirectory = "/it/" + timeString
        _isLocal = false
        _hdfsUrl = x
      })
      .getOrElse({
        val file = createTempDir(this.getClass.getSimpleName)
        tmpFile = Option(file)
        _tmpDirectory = file.getAbsolutePath
        _isLocal = true
        _hdfsUrl = "file://" + _tmpDirectory
      })

    new Hdfs {
      override val hdfsURL: String = _hdfsUrl
      override val tmpDirectory: String = _tmpDirectory
      override val isLocal: Boolean = _isLocal
      override def fileSystem(): FileSystem = {
        val config = new Configuration()
        config.set("fs.defaultFS", hdfsURL)
        FileSystem.get(config)
      }

      override protected def doClose(): Unit = {
        //delete localfile
        tmpFile
          .map(deleteFile(_))
          //delete remoteFile
          .getOrElse({
            val fs = fileSystem()
            fs.delete(new Path(tmpDirectory), true)
          })
      }
    }
  }
}
