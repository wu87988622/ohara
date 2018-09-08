package com.island.ohara.data

import java.util.Objects

import scala.collection.immutable.Set

private class RowBuilderImpl extends RowBuilder {
  private[this] var cells: Seq[Cell[_]] = Seq.empty
  private[this] var tags: Set[String] = Set.empty

  override def build(): Row = {
    Objects.requireNonNull(cells)
    Objects.requireNonNull(tags)
    if (cells.isEmpty) throw new IllegalArgumentException("empty cells!!!!")
    if (cells.map(_.name).toSet.size != cells.length)
      throw new IllegalArgumentException(s"duplicate column:${cells.map(_.name).mkString(",")} are not supported")
    new Row() {
      override def size: Int = cells.length

      override def iterator: Iterator[Cell[_]] = cells.iterator

      override def cell(name: String): Cell[_] = cells.filter(_.name == name).head

      override def names: Iterator[String] = cells.map(_.name).iterator

      override def cell(index: Int): Cell[_] = cells(index)

      override def tags: Set[String] = RowBuilderImpl.this.tags
    }
  }

  override def cells(cells: Seq[Cell[_]]): RowBuilder = {
    this.cells = cells
    this
  }

  override def tags(tags: Set[String]): RowBuilder = {
    this.tags = tags
    this
  }
}
