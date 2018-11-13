package com.island.ohara.connector.ftp
import java.io.{BufferedWriter, OutputStreamWriter}

import com.island.ohara.client.FtpClient
import com.island.ohara.client.util.CloseOnce
import com.island.ohara.connector.ftp.FtpSinkTask._
import com.island.ohara.kafka.connector._
import com.typesafe.scalalogging.Logger

import scala.collection.JavaConverters._
class FtpSinkTask extends RowSinkTask {
  private[this] var config: TaskConfig = _
  private[this] var props: FtpSinkTaskProps = _
  private[this] var ftpClient: FtpClient = _
  override protected def _start(config: TaskConfig): Unit = {
    this.config = config
    this.props = FtpSinkTaskProps(config.options)
    this.ftpClient =
      FtpClient.builder().hostname(props.host).port(props.port).user(props.user).password(props.password).build()
  }

  override protected def _stop(): Unit = {
    CloseOnce.close(ftpClient)
  }

  override protected def _put(records: Seq[RowSinkRecord]): Unit = try {
    val result = records
    // process only matched column name
      .filter(record => config.schema.map(_.name).forall(name => record.row.cells().asScala.exists(_.name == name)))
      // to line
      .map(record => {
        (record,
         record.row
           .cells()
           .asScala
           // pass if there is no schema
           .filter(c => config.schema.isEmpty || config.schema.exists(_.name == c.name))
           //
           .zipWithIndex
           .map {
             case (c, index) =>
               (if (config.schema.isEmpty) index else config.schema.find(_.name == c.name).get.order, c.value)
           }
           .sortBy(_._1)
           .map(_._2.toString)
           .mkString(","))
      })
      // NOTED: we don't want to write an "empty" line
      .filter(_._2.nonEmpty)
    if (result.nonEmpty) {
      val needHeader = props.needHeader && !ftpClient.exist(props.output)
      val writer = new BufferedWriter(
        new OutputStreamWriter(if (ftpClient.exist(props.output)) ftpClient.append(props.output)
                               else ftpClient.create(props.output),
                               props.encode.getOrElse("UTF-8")))
      if (needHeader) {
        val header =
          if (config.schema.nonEmpty) config.schema.sortBy(_.order).map(_.newName).mkString(",")
          else result.head._1.row.cells().asScala.map(_.name).mkString(",")
        writer.append(header)
        writer.newLine()
      }
      try result.foreach {
        case (r, line) =>
          writer.append(line)
          writer.newLine()
      } finally writer.close()
    }
  } catch {
    case e: Throwable => LOG.error("failed to parse records", e)
  }
}

object FtpSinkTask {
  val LOG: Logger = Logger(classOf[FtpSinkTask])
}
