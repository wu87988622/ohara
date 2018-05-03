package com.island.ohara.core

import scala.collection.TraversableOnce
import scala.collection.mutable.ArrayBuffer

private class RowBuilderImpl extends RowBuilder {
  private[this] val cellBuffer = new ArrayBuffer[Cell[_]]()

  override def +=(cell: Cell[_]): this.type = {
    cellBuffer += cell
    this
  }

  override def replace(oldOne: Cell[_], newOne: Cell[_]): this.type = {
    val index = cellBuffer.indexOf(oldOne)
    if (index >= 0) cellBuffer.update(index, newOne)
    else cellBuffer += newOne
    this
  }

  override def clear(): this.type = {
    cellBuffer.clear()
    this
  }

  override def build(): Row = Row(cellBuffer)

  override def -=(cell: Cell[_]): this.type = {
    cellBuffer -= cell
    this
  }

  override def ++=(cells: TraversableOnce[Cell[_]]): RowBuilderImpl.this.type = {
    cellBuffer ++= cells
    this
  }
}
