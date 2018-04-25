package com.island.ohara.core

import com.island.ohara.core.CellBuilder.State

/**
  * A cell consists of name and value.
  *
  * @tparam T value type
  */
abstract class Cell[T] {
  def name: String

  def value: T

  override def toString = s"$name/${value.toString}"
}

object Cell {
  // TODO: use offheap builder instead
  def builder: CellBuilder[State] = new CellBuilderImpl[State]()
}