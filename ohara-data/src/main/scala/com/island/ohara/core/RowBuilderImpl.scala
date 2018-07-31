package com.island.ohara.core

import scala.collection.immutable.Set
import scala.collection.mutable.ArrayBuffer

private class RowBuilderImpl extends RowBuilder {
  private[this] val cellBuffer = new ArrayBuffer[Cell[_]]()
  private[this] var tags: Set[String] = Set.empty

  override def append(cell: Cell[_]): RowBuilder = {
    cellBuffer += cell
    this
  }

  override def replace(oldOne: Cell[_], newOne: Cell[_]): RowBuilder = {
    val index = cellBuffer.indexOf(oldOne)
    if (index >= 0) cellBuffer.update(index, newOne)
    else cellBuffer += newOne
    this
  }

  override def clear(): RowBuilder = {
    cellBuffer.clear()
    this
  }

  override def build(): Row = Row(cellBuffer, tags)

  override def remove(cell: Cell[_]): RowBuilder = {
    cellBuffer -= cell
    this
  }

  override def append(cells: TraversableOnce[Cell[_]]): RowBuilder = {
    cellBuffer ++= cells
    this
  }

  override def tags(tags: Set[String]): RowBuilder = {
    this.tags = tags
    this
  }
}
