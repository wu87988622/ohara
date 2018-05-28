package com.island.ohara.serialization

/**
  * a do nothing serializer.
  */
object BytesSerializer extends Serializer[Array[Byte]] {

  override def to(obj: Array[Byte]): Array[Byte] = obj

  override def from(serial: Array[Byte]): Array[Byte] = serial
}
