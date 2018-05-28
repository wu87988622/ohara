package com.island.ohara.serialization

import com.island.ohara.io.ByteUtil

/**
  * Used to do conversion between Float and byte array.
  */
object FloatSerializer extends Serializer[Float] {

  override def to(obj: Float): Array[Byte] = ByteUtil.toBytes(obj)

  override def from(serial: Array[Byte]): Float = ByteUtil.toFloat(serial)
}
