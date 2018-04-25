package com.island.ohara.serialization

import java.io.OutputStream

import com.island.ohara.core.{Row, Table}
import com.island.ohara.io.CloseOnce.doClose
import com.island.ohara.io.{ArrayBufferOutputStream, DataStreamWriter}

/**
  * Used to serialize a Table to specified output
  */
abstract class TableWriter extends AutoCloseable {
  def startRow(cellCount: Int): RowWriter
}

object TableWriter {

  def toBytes(table: Table): Array[Byte] = {
    doClose(new ArrayBufferOutputStream(table.cellCount * 20)) {
      output =>
        doClose(TableWriter(output, table.id, table.rowCount)) {
          tableWriter =>
            table.foreach((row: Row) => doClose(tableWriter.startRow(row.cellCount))
            (rowWriter => row.foreach(rowWriter.append(_))))
            output.toByteArray
        }
    }
  }

  /**
    * Return a TableWriter used to write the table to output stream.
    * NOTED: the output stream is closed automatically
    *
    * @param output   output stream
    * @param id       table id
    * @param rowCount row count
    * @return a TableWriter
    */
  def apply(output: OutputStream, id: String, rowCount: Int): TableWriter = apply(DataStreamWriter(output), id, rowCount)

  /**
    * Return a TableWriter used to write the table to output stream.
    * NOTED: the output stream is closed automatically
    *
    * @param writer   output writer
    * @param id       table id
    * @param rowCount row count
    * @return a TableWriter
    */
  def apply(writer: DataStreamWriter, id: String, rowCount: Int, autoClose: Boolean = true): TableWriter = {
    writer.write(0)
    new TableWriterImpl(writer, id, rowCount, autoClose)
  }
}