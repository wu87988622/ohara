package com.island.ohara.configurator.store

import com.island.ohara.configurator.serialization.Serializer
import com.island.ohara.io.ByteUtil

class StringSerializer extends Serializer[String, Array[Byte]] {

  override def to(obj: String): Array[Byte] = ByteUtil.toBytes(obj)

  override def from(serial: Array[Byte]): String = ByteUtil.toString(serial)
}
