package com.island.ohara.connector.hdfs.text

import java.io.OutputStream

import com.island.ohara.serialization.DataType
import com.island.ohara.client.ConfiguratorJson.Column
import com.island.ohara.connector.hdfs.storage.Storage
import com.island.ohara.data.Row
import com.island.ohara.io.ByteUtil
import com.typesafe.scalalogging.Logger

/**
  * Data write to temp file for csv file format
  * @param storage
  */
class CSVRecordWriterOutput(storage: Storage, filePath: String) extends RecordWriterOutput {
  private[this] lazy val logger = Logger(getClass.getName)
  logger.info("open temp file")
  val outputStream: OutputStream = storage.open(filePath, false)

  /**
    * Write file for csv format
    * @param row
    */
  override def write(isHeader: Boolean, schema: Seq[Column], row: Row): Unit = {
    val newSchema: Seq[Column] = if (schema.isEmpty) row.map(r => Column(r.name, DataType.OBJECT, 0)).toSeq else schema
    if (isHeader) {
      val header: String = newSchema.sortBy(_.order).map(s => s.newName).mkString(",")
      this.outputStream.write(ByteUtil.toBytes(header + "\n"))
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
    this.outputStream.write(ByteUtil.toBytes(line + "\n"))
  }

  /**
    * close OutputStream object
    */
  override def close(): Unit = {
    logger.info("close temp file")
    this.outputStream.close()
  }
}
