package com.island.ohara.serialization

import com.island.ohara.io.ByteUtil

/**
  * Used to do conversion between Short and byte array.
  */
object ShortSerializer extends Serializer[Short] {

  override def to(obj: Short): Array[Byte] = ByteUtil.toBytes(obj)

  override def from(serial: Array[Byte]): Short = ByteUtil.toShort(serial)
}
