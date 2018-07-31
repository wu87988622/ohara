package com.island.ohara.serialization

import com.island.ohara.io.ByteUtil

/**
  * Used to do conversion between Double and byte array.
  */
object DoubleSerializer extends Serializer[Double] {

  override def to(obj: Double): Array[Byte] = ByteUtil.toBytes(obj)

  override def from(serial: Array[Byte]): Double = ByteUtil.toDouble(serial)
}
