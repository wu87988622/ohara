/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.island.ohara.connector.hdfs.text

import java.io.{BufferedWriter, OutputStreamWriter, Writer}

import com.island.ohara.common.data.{Column, DataType, Row}
import com.island.ohara.common.util.Releasable
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
      if (schema.isEmpty)
        row.cells().asScala.map(r => Column.newBuilder().name(r.name).dataType(DataType.OBJECT).order(0).build())
      else schema
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
    Releasable.close(writer)
  }

  private[this] def writerHeader(writer: Writer, newSchema: Seq[Column], isHeader: Boolean): Unit = {
    if (isHeader) {
      val header: String = newSchema.sortBy(_.order).map(s => s.newName).mkString(",")
      this.writer.append(header)
      this.writer.newLine()
    }
  }
}
