package com.island.ohara.connector.ftp

import com.island.ohara.client.ConfiguratorJson.Column
import com.island.ohara.connector.ftp.FtpSource.LOG
import com.island.ohara.connector.ftp.FtpSourceTask._
import com.island.ohara.data.{Cell, Row}
import com.island.ohara.io.{CloseOnce, IoUtil}
import com.island.ohara.kafka.connector.{RowSourceContext, RowSourceRecord, RowSourceTask, TaskConfig}
import com.island.ohara.serialization.DataType

import scala.collection.mutable

/**
  * Move files from FTP server to Kafka topics. The file format must be csv file, and element in same line must be separated
  * by comma. The offset is (path, line index). It means each line is stored as a "message" in connector topic. For example:
  * a file having 100 lines has 100 message in connector topic. If the file is processed correctly, the TestFtpSource
  */
class FtpSourceTask extends RowSourceTask {

  private[this] var props: FtpSourceTaskProps = _
  private[this] var topics: Seq[String] = _
  private[this] var schema: Seq[Column] = _
  private[this] var ftpClient: FtpClient = _
  private[this] var offsets: Offsets = _
  private[this] val files = new mutable.Queue[String]()

  override protected def _stop(): Unit = {
    CloseOnce.close(ftpClient)
  }

  override protected def _poll(): Seq[RowSourceRecord] = {
    def handleError(path: String): Unit = try {
      ftpClient.moveFile(path, IoUtil.path(props.error, IoUtil.name(path)))
    } catch {
      case e: Throwable => LOG.error(s"failed to move $path to ${props.error}", e)
    }

    def handleOutput(path: String): Unit = try {
      ftpClient.moveFile(path, IoUtil.path(props.output, IoUtil.name(path)))
      offsets.remove(path)
    } catch {
      case e: Throwable => LOG.error(s"failed to move $path to ${props.output}", e)
    }

    def list(folder: String): Seq[String] =
      ftpClient.listFileNames(folder).map(IoUtil.path(folder, _)).filter(_.hashCode % props.total == props.hash)

    def toRow(line: String): Row = {
      val splits = line.split(",")
      if (splits.length < schema.size) throw new IllegalStateException(s"schema doesn't match to $line")
      Row(
        schema
          .sortBy(_.order)
          .map(column => {
            // column order start from 1 rather than 0
            val item = splits(column.order - 1)
            Cell(
              column.name,
              column.typeName match {
                case DataType.BOOLEAN => item.toBoolean
                case DataType.INT     => item.toInt
                case DataType.LONG    => item.toLong
                case DataType.FLOAT   => item.toFloat
                case DataType.DOUBLE  => item.toDouble
                case DataType.STRING  => item
                // TODO: should we convert bytes?
                case _ => throw new IllegalArgumentException("Unsupported type...")
              }
            )
          }): _*)
    }

    try if (files.isEmpty) files ++= list(props.input)
    catch {
      case e: Throwable => LOG.error(s"failed to list ${props.input}", e)
    }
    if (files.isEmpty) Seq.empty
    else {
      while (files.nonEmpty) {
        val path = files.dequeue()
        try {
          val lineAndIndex = ftpClient.readLines(path, props.encode.getOrElse("UTF-8")).zipWithIndex.filter {
            case (_, index) => offsets.predicate(path, index)
          }
          val records: Seq[RowSourceRecord] = lineAndIndex.flatMap {
            case (line, index) =>
              val p = partition(path)
              val o = offset(index)
              val r = toRow(line)
              topics.map(t => {
                RowSourceRecord.builder.sourcePartition(p).sourceOffset(o).topic(t).row(r).build()
              })
          }
          if (records.nonEmpty) {
            // update cache
            lineAndIndex.map(_._2).foreach(offsets.update(path, _))
            return records
          } else {
            // all lines in this file are processed
            handleOutput(path)
          }
        } catch {
          case e: Throwable => {
            LOG.error(s"failed to handle $path", e)
            handleError(path)
            Seq.empty
          }
        }
      }
    }
    Seq.empty
  }
  override protected def _version: String = FtpSource.VERSION
  override protected def _start(config: TaskConfig): Unit = {
    this.props = FtpSourceTaskProps(config.options)
    this.schema = config.schema
    if (props.input.isEmpty) throw new IllegalArgumentException(s"invalid input:${props.input.mkString(",")}")
    topics = config.topics
    ftpClient = FtpClient.builder.host(props.host).port(props.port).user(props.user).password(props.password).build()
    offsets = new Offsets(rowContext)
  }

  private class Offsets(context: RowSourceContext) {

    private[this] val cache = new mutable.HashMap[String, Int]()

    def remove(path: String): Unit = cache.remove(path)

    def update(path: String, index: Int): Unit = {
      val previous = cache.getOrElseUpdate(path, index)
      if (index > previous) cache.update(path, index)
    }

    def predicate(path: String, index: Int): Boolean = predicate(path, index, true)

    private[this] def predicate(path: String, index: Int, needUpdate: Boolean): Boolean = {
      val pass = cache.get(path).forall(index > _)
      if (needUpdate && pass) {
        val map = context.offset(partition(path))
        if (map.nonEmpty) update(path, offset(map))
        predicate(path, index, false)
      } else pass
    }
  }
}

object FtpSourceTask {
  private[this] val PATH_KEY = "ftp.file.path"
  private[this] val LINE_KEY = "ftp.file.offset"
  def partition(path: String): Map[String, _] = Map(PATH_KEY -> path)
  def offset(index: Int): Map[String, _] = Map(LINE_KEY -> index.toString)
  def offset(o: Map[String, _]): Int = o(LINE_KEY).asInstanceOf[String].toInt
}
