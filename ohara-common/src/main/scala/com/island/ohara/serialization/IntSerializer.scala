package com.island.ohara.serialization

import com.island.ohara.io.ByteUtil

/**
  * Used to do conversion between Int and byte array.
  */
object IntSerializer extends Serializer[Int] {

  override def to(obj: Int): Array[Byte] = ByteUtil.toBytes(obj)

  override def from(serial: Array[Byte]): Int = ByteUtil.toInt(serial)
}
