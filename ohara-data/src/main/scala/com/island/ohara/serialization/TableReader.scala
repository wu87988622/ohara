package com.island.ohara.serialization

import java.io.{ByteArrayInputStream, InputStream}

import com.island.ohara.core.{Row, Table}
import com.island.ohara.io.CloseOnce.doClose
import com.island.ohara.io.DataStreamReader

/**
  * Used to create a Table from other resources
  */
abstract class TableReader extends Iterator[Row] with AutoCloseable {
  def id: String

  def rowCount: Int
}

object TableReader {

  /**
    * Instantiate a table by a byte array.
    *
    * @param buffer byte array storing the table data
    * @return a Table
    */
  def toTable(buffer: Array[Byte]): Table =
    doClose(TableReader(buffer))(tableReader => (Table.builder(tableReader.id) ++= tableReader).build())

  /**
    * Create a TableReader used to build a Table from a byte array
    *
    * @param buffer byte array
    * @return a TableReader
    */
  def apply(buffer: Array[Byte]): TableReader = apply(new ByteArrayInputStream(buffer))

  /**
    * Create a TableReader used to build a Table from a input stream
    *
    * @param input bytes input
    * @return a TableReader
    */
  def apply(input: InputStream): TableReader = apply(new DataStreamReader(input))

  /**
    * Create a TableReader used to build a Table from a DataStreamReader
    *
    * @param reader    object reader
    * @param autoClose true: the input will be closed with TableReader#close
    * @return a TableReader
    */
  def apply(reader: DataStreamReader, autoClose: Boolean = true): TableReader = {
    reader.readInt() match {
      case 0      => new TableReaderImpl(reader, autoClose)
      case v: Int => throw new UnsupportedOperationException(s"unsupported version:$v")
    }
  }
}
