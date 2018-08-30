package com.island.ohara.data

import java.util.Objects

import com.island.ohara.io.ByteUtil

private class CellBuilderImpl extends CellBuilder {
  private[this] var name: String = _

  override def name(name: String): CellBuilder = {
    this.name = name
    this
  }

  override def build[T](v: T): Cell[T] = {
    if (name == null || name.isEmpty) throw new NullPointerException("The cell name can't be null or empty")
    Objects.requireNonNull(v)
    v match {
      // enable the byte array to be comparable
      case bytes: Array[Byte] =>
        new Cell[T]() {
          override def name: String = CellBuilderImpl.this.name

          override def value: T = v

          override def equals(obj: Any): Boolean = obj match {
            case another: Cell[_] =>
              another.value match {
                case anotherBytes: Array[Byte] => ByteUtil.compare(bytes, anotherBytes) == 0
                case _                         => false
              }
            case _ => false
          }
        }
      case _ =>
        new Cell[T]() {
          override def name: String = CellBuilderImpl.this.name

          override def value: T = v
        }
    }
  }
}
