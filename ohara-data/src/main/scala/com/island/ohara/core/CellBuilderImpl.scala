package com.island.ohara.core

import com.island.ohara.core.CellBuilder.{FullState, Name, State}

private class CellBuilderImpl[state <: State] extends CellBuilder[state] {
  private[this] var name: String = _

  override def name(name: String): CellBuilder[state with Name] = {
    this.name = name
    this.asInstanceOf[CellBuilder[state with Name]]
  }

  override def build[T](v: T)(implicit state: state =:= FullState) = new Cell[T]() {
    override def name: String = CellBuilderImpl.this.name

    override def value: T = v
  }
}
