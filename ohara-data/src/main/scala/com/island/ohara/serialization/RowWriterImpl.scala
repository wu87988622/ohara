package com.island.ohara.serialization

import com.island.ohara.core.Cell
import com.island.ohara.io.{ByteUtil, CloseOnce, DataStreamWriter}

/**
  *
  * cell count of first row (4 bytes)
  * cell length (4 bytes) | cell name length (2 bytes) | cell name | cell value type (1 byte) | cell value length (2 bytes) | cell value
  * cell length (4 bytes) | cell name length (2 bytes) | cell name | cell value type (1 byte) | cell value length (2 bytes) | cell value
  *
  * @param writer
  * @param cellCount
  * @param autoClose
  */
private class RowWriterImpl(val writer: DataStreamWriter, cellCount: Int, autoClose: Boolean)
    extends RowWriter
    with CloseOnce {
  writer.write(cellCount)
  private[this] var processedCellCount: Int = 0

  private[this] def checkState(): Unit = {
    checkClose
    if (processedCellCount >= cellCount) throw new IllegalArgumentException(s"there are no rooms to accept more cells")
  }

  override def append(cell: Cell[_]): RowWriter = {
    checkState()
    val (valueType, valueBytes) = cell.value match {
      case v: Array[Byte] => (BYTES, v)
      case v: Boolean     => (BOOLEAN, ByteUtil.toBytes(v))
      case v: Short       => (SHORT, ByteUtil.toBytes(v))
      case v: Int         => (INT, ByteUtil.toBytes(v))
      case v: Long        => (LONG, ByteUtil.toBytes(v))
      case v: Float       => (FLOAT, ByteUtil.toBytes(v))
      case v: Double      => (DOUBLE, ByteUtil.toBytes(v))
      case v: String      => (STRING, ByteUtil.toBytes(v))
      case v: Any         => throw new UnsupportedClassVersionError(s"class:${v.getClass.getName}")
    }
    val nameBytes = ByteUtil.toBytes(cell.name)
    val cellLength = TableWriterImpl.CELL_OVERHEAD + nameBytes.length + valueBytes.length
    writer.write(cellLength)
    // convert the int to short
    writer.write(nameBytes.length.toShort)
    writer.write(nameBytes)
    writer.write(valueType.index)
    // convert the int to short
    writer.write(valueBytes.length.toShort)
    writer.write(valueBytes)
    processedCellCount += 1
    this
  }

  override protected def doClose(): Unit = if (autoClose) writer.close()
}
