package com.island.ohara.serialization

import com.island.ohara.core.Cell
import com.island.ohara.io.{ByteUtil, CloseOnce, DataStreamWriter}

/**
  * A simple impl to TableWriter. It is a private class since user should use the trait rather than impl
  * id length (2 bytes)
  * table id
  * row count (4 bytes)
  * first record
  * second record
  * ...
  *
  * @param writer   the data output
  * @param id       id of table
  * @param rowCount number of rows
  */
private class TableWriterImpl(val writer: DataStreamWriter, id: String, rowCount: Int, autoClose: Boolean) extends TableWriter with CloseOnce {
  if (id == null || id.length == 0) throw new IllegalArgumentException("Unsupported to pass a empty id")
  val idBytes = ByteUtil.toBytes(id)
  // NOTED: 2 bytes is enough to store the id
  writer.write(idBytes.length.toShort)
  writer.write(idBytes)
  writer.write(rowCount)
  private[this] var rowIndex = 0

  override def startRow(cellCount: Int): RowWriter = {
    checkClose
    if (rowIndex >= rowCount) throw new IllegalArgumentException(s"there are no rooms to accept more rows")
    val current = RowWriter(writer, cellCount, false)
    rowIndex += 1
    current
  }

  override protected def doClose(): Unit = if (autoClose) writer.close()
}

private[serialization] object TableWriterImpl {
  val CELL_OVERHEAD
  = ByteUtil.SIZE_OF_INT // cell length
  +ByteUtil.SIZE_OF_SHORT // cell name length
  +ByteUtil.SIZE_OF_BYTE // cell value type
  +ByteUtil.SIZE_OF_SHORT // cell value length
}