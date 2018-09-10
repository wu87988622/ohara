package com.island.ohara.connector.ftp
import java.io.{BufferedWriter, OutputStreamWriter}

import com.island.ohara.io.CloseOnce
import com.island.ohara.kafka.connector._
import com.typesafe.scalalogging.Logger

import scala.collection.mutable
import FtpSinkTask._
class FtpSinkTask extends RowSinkTask {
  private[this] var config: TaskConfig = _
  private[this] var props: FtpSinkTaskProps = _
  private[this] var ftpClient: FtpClient = _
  private[this] val committedOffsets = new mutable.HashSet[TopicPartition]()
  override protected def _start(config: TaskConfig): Unit = {
    this.config = config
    this.props = FtpSinkTaskProps(config.options)
    this.ftpClient =
      FtpClient.builder().host(props.host).port(props.port).user(props.user).password(props.password).build()
  }

  override protected def _stop(): Unit = {
    CloseOnce.close(ftpClient)
  }

  override protected def _put(records: Seq[RowSinkRecord]): Unit = try {
    val result = records
    // process only primitive type
      .filter(_.row.forall(_.value match {
        case Short | Int | Long | Float | Double | Boolean => true
        case _: java.lang.Number                           => true
        case _: java.lang.Boolean                          => true
        case _: String                                     => true
        case _                                             => false
      }))
      // to line
      .map(record => {
        (record,
         record.row
           .filter(c => config.schema.exists(_.name == c.name))
           .map(c => (config.schema.find(_.name == c.name).get, c.value))
           .toSeq
           .sortBy(_._1.order)
           .map(_._2.toString)
           .mkString(","))
      })
      // NOTED: we don't want to write an "empty" line
      .filter(_._2.nonEmpty)
    if (result.nonEmpty) {
      val writer = new BufferedWriter(
        new OutputStreamWriter(ftpClient.create(props.output), props.encode.getOrElse("UTF-8")))
      try result.foreach {
        case (r, line) => {
          writer.append(line)
          writer.newLine()
          committedOffsets += TopicPartition(r.topic, r.partition)
        }
      } finally writer.close()
    }
  } catch {
    case e: Throwable => LOG.error("failed to parse records", e)
  }

  override protected def _preCommit(offsets: Map[TopicPartition, TopicOffset]): Map[TopicPartition, TopicOffset] =
    offsets.filter {
      case (p, _) => committedOffsets.remove(p)
    }

  override protected def _version: String = VERSION
}

object FtpSinkTask {
  val LOG: Logger = Logger(classOf[FtpSinkTask])
}
