package com.island.ohara.core

import scala.collection.TraversableOnce
import scala.collection.mutable.ArrayBuffer

private class TableBuilderImpl(id: String) extends TableBuilder {
  private[this] val rowBuffer = new ArrayBuffer[Row]()

  override def +=(row: Row): this.type = {
    rowBuffer += row
    this
  }

  override def -=(row: Row): this.type = {
    rowBuffer -= row
    this
  }

  override def replace(oldOne: Row, newOne: Row): this.type = {
    val index = rowBuffer.indexOf(oldOne)
    if (index >= 0) rowBuffer.update(index, newOne)
    else rowBuffer += newOne
    this
  }

  override def clear(): this.type = {
    rowBuffer.clear()
    this
  }

  override def build(): Table = Table(id, rowBuffer)

  override def ++=(rows: TraversableOnce[Row]): TableBuilderImpl.this.type = {
    rowBuffer ++= rows
    this
  }
}
