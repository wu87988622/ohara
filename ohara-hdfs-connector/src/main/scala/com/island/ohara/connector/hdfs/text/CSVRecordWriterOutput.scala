package com.island.ohara.connector.hdfs.text

import java.io.{BufferedWriter, OutputStreamWriter}
import com.island.ohara.serialization.DataType
import com.island.ohara.client.ConfiguratorJson.Column
import com.island.ohara.connector.hdfs.HDFSSinkConnectorConfig
import com.island.ohara.connector.hdfs.storage.Storage
import com.island.ohara.data.Row
import com.typesafe.scalalogging.Logger

/**
  * Data write to temp file for csv file format
  * @param storage
  */
class CSVRecordWriterOutput(hdfsSinkConnectorConfig: HDFSSinkConnectorConfig, storage: Storage, filePath: String)
    extends RecordWriterOutput {
  private[this] lazy val logger = Logger(getClass.getName)
  logger.info("open temp file")
  val encode = hdfsSinkConnectorConfig.dataFileEncode
  val writer: BufferedWriter = new BufferedWriter(new OutputStreamWriter(storage.open(filePath, false), encode))

  /**
    * Write file for csv format
    * @param row
    */
  override def write(isHeader: Boolean, schema: Seq[Column], row: Row): Unit = {
    val newSchema: Seq[Column] = if (schema.isEmpty) row.map(r => Column(r.name, DataType.OBJECT, 0)).toSeq else schema
    if (isHeader) {
      val header: String = newSchema.sortBy(_.order).map(s => s.newName).mkString(",")
      this.writer.append(header)
      this.writer.newLine()
    }

    val line = newSchema
      .sortBy(_.order)
      .map(n => (n, row.filter(r => n.name == r.name)))
      .flatMap({
        case (n, cell) =>
          n.typeName match {
            case DataType.BYTES | DataType.BYTE =>
              throw new RuntimeException(s"hdfs sink connector not support ${n.typeName} type")
            case _ => cell
          }
      })
      .map(x => x.value)
      .mkString(",")

    this.logger.debug(s"data line: $line")
    this.writer.append(line)
    this.writer.newLine()
  }

  /**
    * close OutputStream object
    */
  override def close(): Unit = {
    logger.info("close temp file")
    this.writer.close()
  }
}
