package com.island.ohara.serialization

import com.island.ohara.io.ByteUtil

/**
  * Used to do conversion between String and byte array.
  */
object StringSerializer extends Serializer[String] {

  override def to(obj: String): Array[Byte] = ByteUtil.toBytes(obj)

  override def from(serial: Array[Byte]): String = ByteUtil.toString(serial)
}
