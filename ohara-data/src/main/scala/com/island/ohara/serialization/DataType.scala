package com.island.ohara.serialization

/**
  * List the supported type in default reader/writer.
  * NOTED: DON'T change the index since it is a part of serialization.
  */
sealed trait DataType {
  def index: Byte
}

case object BYTES extends DataType {
  override def index: Byte = 0
}

case object BOOLEAN extends DataType {
  override def index: Byte = 1
}

case object BYTE extends DataType {
  override def index: Byte = 2
}

case object SHORT extends DataType {
  override def index: Byte = 3
}

case object INT extends DataType {
  override def index: Byte = 4
}

case object LONG extends DataType {
  override def index: Byte = 5
}

case object FLOAT extends DataType {
  override def index: Byte = 6
}

case object DOUBLE extends DataType {
  override def index: Byte = 7
}

case object STRING extends DataType {
  override def index: Byte = 8
}

object DataType {
  def all = Array(BYTES, BOOLEAN, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, STRING)

  def of(index: Byte): DataType = all.find(_.index == index).get
}
