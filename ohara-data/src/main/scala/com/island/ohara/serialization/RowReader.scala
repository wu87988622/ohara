package com.island.ohara.serialization

import java.io.{ByteArrayInputStream, InputStream}

import com.island.ohara.core.{Cell, Row}
import com.island.ohara.io.CloseOnce.doClose
import com.island.ohara.io.DataStreamReader

abstract class RowReader extends Iterator[Cell[_]] with AutoCloseable {
  def cellCount: Int
}

object RowReader {
  /**
    * Instantiate a table by a byte array.
    *
    * @param buffer byte array storing the table data
    * @return a Table
    */
  def toRow(buffer: Array[Byte]): Row = doClose(RowReader(buffer))(rowReader => (Row.builder ++= rowReader).build())

  /**
    * Create a TableReader used to build a Table from a byte array
    *
    * @param buffer byte array
    * @return a TableReader
    */
  def apply(buffer: Array[Byte]): RowReader = apply(new ByteArrayInputStream(buffer))

  /**
    * Create a TableReader used to build a Table from a input stream
    *
    * @param input bytes input
    * @return a TableReader
    */
  def apply(input: InputStream): RowReader = apply(new DataStreamReader(input))

  /**
    * Create a TableReader used to build a Table from a DataStreamReader
    *
    * @param reader    object reader
    * @param autoClose true: the input will be closed with TableReader#close
    * @return a TableReader
    */
  def apply(reader: DataStreamReader, autoClose: Boolean = true): RowReader = {
    reader.readInt() match {
      case 0 => new RowReaderImpl(reader, autoClose)
      case v: Int => throw new UnsupportedOperationException(s"unsupported version:$v")
    }
  }
}