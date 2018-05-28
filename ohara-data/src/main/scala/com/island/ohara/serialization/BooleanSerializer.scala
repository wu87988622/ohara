package com.island.ohara.serialization

import com.island.ohara.io.ByteUtil

/**
  * Used to do conversion between Boolean and byte array.
  */
object BooleanSerializer extends Serializer[Boolean] {

  override def to(obj: Boolean): Array[Byte] = ByteUtil.toBytes(obj)

  override def from(serial: Array[Byte]): Boolean = ByteUtil.toBoolean(serial)
}
