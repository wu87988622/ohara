package com.island.ohara.data

import java.util.Objects

private class CellBuilderImpl extends CellBuilder {
  private[this] var name: String = _

  override def name(name: String): CellBuilder = {
    this.name = name
    this.asInstanceOf[CellBuilder]
  }

  override def build[T](v: T) = {
    if (name == null || name.isEmpty) throw new NullPointerException("The cell name can't be null or empty")
    Objects.requireNonNull(v)
    new Cell[T]() {
      override def name: String = CellBuilderImpl.this.name

      override def value: T = v
    }
  }
}
