package com.island.ohara.connector.hdfs.text

import java.io.{BufferedWriter, OutputStreamWriter, Writer}

import com.island.ohara.client.ConfiguratorJson.Column
import com.island.ohara.client.util.CloseOnce
import com.island.ohara.common.data.{DataType, Row}
import com.island.ohara.connector.hdfs.HDFSSinkConnectorConfig
import com.island.ohara.connector.hdfs.storage.Storage
import com.typesafe.scalalogging.Logger
import scala.collection.JavaConverters._

/**
  * Data write to temp file for csv file format
  * @param storage
  */
class CSVRecordWriterOutput(hdfsSinkConnectorConfig: HDFSSinkConnectorConfig, storage: Storage, filePath: String)
    extends RecordWriterOutput {
  private[this] lazy val logger = Logger(getClass.getName)
  private[this] var writer: BufferedWriter = _
  val encode = hdfsSinkConnectorConfig.dataFileEncode

  /**
    * Write file for csv format
    * @param row
    */
  override def write(isHeader: Boolean, schema: Seq[Column], row: Row): Unit = {
    val newSchema: Seq[Column] =
      if (schema.isEmpty) row.cells().asScala.map(r => Column(r.name, DataType.OBJECT, 0)).toSeq else schema
    val line: String = newSchema
      .sortBy(_.order)
      .map(n => (n, row.cells().asScala.filter(r => n.name == r.name)))
      .flatMap({
        case (n, cell) =>
          n.dataType match {
            case DataType.BYTES =>
              throw new RuntimeException(s"hdfs sink connector not support ${n.dataType} type")
            case _ => cell
          }
      })
      .map(x => x.value)
      .mkString(",")

    if (this.writer == null && line.nonEmpty) {
      logger.info("open temp file")
      this.writer = new BufferedWriter(new OutputStreamWriter(storage.open(filePath, false), encode))
      writerHeader(this.writer, newSchema, isHeader)
    }

    if (line.nonEmpty) {
      this.writer.append(line)
      this.writer.newLine()
    }
  }

  /**
    * close OutputStream object
    */
  override def close(): Unit = {
    logger.info("close temp file")
    CloseOnce.close(writer)
  }

  private[this] def writerHeader(writer: Writer, newSchema: Seq[Column], isHeader: Boolean): Unit = {
    if (isHeader) {
      val header: String = newSchema.sortBy(_.order).map(s => s.newName).mkString(",")
      this.writer.append(header)
      this.writer.newLine()
    }
  }
}
