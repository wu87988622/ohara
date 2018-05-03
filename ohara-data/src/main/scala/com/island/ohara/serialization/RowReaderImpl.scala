package com.island.ohara.serialization

import com.island.ohara.core.Cell
import com.island.ohara.io.{ByteUtil, CloseOnce, DataStreamReader}

private class RowReaderImpl(reader: DataStreamReader, autoClose: Boolean) extends RowReader with CloseOnce {
  private[this] var nextCell: Cell[_] = null
  private[this] var cellIndex: Int = 0
  val cellCount = reader.readInt()
  if (cellCount < 0) throw new IllegalStateException(s"the number of cell is $cellCount. It should be bigger than zero")

  override protected def doClose(): Unit = if (autoClose) reader.close()

  override def hasNext: Boolean = {
    if (nextCell != null) return true
    if (cellIndex >= cellCount) return false
    // TODO: we know the size of cell so it is doable to locate the cell at single byte array
    reader.readInt()

    implicit def readValue = (reader: DataStreamReader) => reader.forceRead(reader.readShort())

    val name = ByteUtil.toString(reader)
    nextCell = DataType.of(reader.readByte()) match {
      case BYTES   => Cell.builder.name(name).build(reader)
      case BOOLEAN => Cell.builder.name(name).build(ByteUtil.toBoolean(reader))
      case SHORT   => Cell.builder.name(name).build(ByteUtil.toShort(reader))
      case INT     => Cell.builder.name(name).build(ByteUtil.toInt(reader))
      case LONG    => Cell.builder.name(name).build(ByteUtil.toLong(reader))
      case FLOAT   => Cell.builder.name(name).build(ByteUtil.toFloat(reader))
      case DOUBLE  => Cell.builder.name(name).build(ByteUtil.toDouble(reader))
      case STRING  => Cell.builder.name(name).build(ByteUtil.toString(reader))
      case e: Any  => throw new UnsupportedClassVersionError(s"${e.getClass.getName}")
    }
    cellIndex += 1
    true
  }

  override def next(): Cell[_] = {
    try if (!hasNext) throw new NoSuchElementException else nextCell
    finally nextCell = null
  }
}
