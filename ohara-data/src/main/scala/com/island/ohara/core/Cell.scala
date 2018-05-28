package com.island.ohara.core

/**
  * A cell consists of name and value.
  *
  * @tparam T value type
  */
abstract class Cell[T] {
  def name: String

  def value: T

  override def toString = s"$name/${value.toString}"

  /**
    * Indicates whether this cell is equal to another cell
    * NOTED: the default implementation depends on the value#quals.
    *
    * @param obj another cell
    * @return true if this cell is equal with another cell. false otherwise
    */
  override def equals(obj: scala.Any): Boolean = obj match {
    case cell: Cell[_] if (name.equals(cell.name)) => value.equals(cell.value)
    case _                                         => false
  }
}

object Cell {
  // TODO: use offheap builder instead. by chia
  def builder: CellBuilder = new CellBuilderImpl()
}
