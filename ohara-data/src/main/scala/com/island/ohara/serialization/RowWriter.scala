package com.island.ohara.serialization

import java.io.OutputStream

import com.island.ohara.core.{Cell, Row}
import com.island.ohara.io.CloseOnce.doClose
import com.island.ohara.io.{ArrayBufferOutputStream, DataStreamWriter}

abstract class RowWriter extends AutoCloseable {
  def append(cell: Cell[_]): RowWriter
}

object RowWriter {
  def toBytes(row: Row): Array[Byte] = {
    doClose(new ArrayBufferOutputStream(row.cellCount * 20)) { output =>
      doClose(RowWriter(output, row.cellCount)) { rowWriter =>
        row.foreach(rowWriter.append(_))
        output.toByteArray
      }
    }
  }

  def apply(output: OutputStream, cellCount: Int): RowWriter = apply(DataStreamWriter(output), cellCount)

  def apply(writer: DataStreamWriter, cellCount: Int, autoClose: Boolean = true): RowWriter = {
    writer.write(0)
    new RowWriterImpl(writer, cellCount, autoClose)
  }
}
