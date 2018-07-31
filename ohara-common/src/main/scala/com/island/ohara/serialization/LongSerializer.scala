package com.island.ohara.serialization

import com.island.ohara.io.ByteUtil

/**
  * Used to do conversion between Long and byte array.
  */
object LongSerializer extends Serializer[Long] {

  override def to(obj: Long): Array[Byte] = ByteUtil.toBytes(obj)

  override def from(serial: Array[Byte]): Long = ByteUtil.toLong(serial)
}
