package com.island.ohara.serialization

import com.island.ohara.core.Row
import com.island.ohara.io.{CloseOnce, DataStreamReader}

/**
  * A simple impl to TableReader. All data are allocated at individual heap.
  * It is a private class since user should use the trait rather than impl
  *
  * @param reader the data input
  */
private class TableReaderImpl(reader: DataStreamReader, autoClose: Boolean) extends TableReader with CloseOnce {
  override val id = reader.readString(reader.readShort())
  override val rowCount = reader.readInt()
  private[this] var rowIndex: Int = 0
  private[this] var nextRow: Row = null

  override def hasNext: Boolean = {
    if (nextRow != null) true
    else {
      if (rowIndex >= rowCount) false
      else {
        nextRow = Row(RowReader(reader, false))
        rowIndex += 1
        true
      }
    }
  }

  override def next(): Row = {
    if (nextRow == null && !hasNext) throw new NoSuchElementException()
    try nextRow finally nextRow = null
  }

  override protected def doClose(): Unit = if (autoClose) reader.close()
}
