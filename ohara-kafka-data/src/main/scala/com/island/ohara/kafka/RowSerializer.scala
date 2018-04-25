package com.island.ohara.kafka

import java.util

import com.island.ohara.core.Row
import com.island.ohara.serialization.RowWriter
import org.apache.kafka.common.serialization.Serializer

/**
  * Used to convert ohara row to byte array. It is a private class since ohara producer will instantiate one and pass it to
  * kafka producer. Hence, no dynamical call will happen in kafka producer. The access exception won't be caused.
  */
class RowSerializer extends Serializer[Row] {
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {
    // do nothing
  }

  override def serialize(topic: String, data: Row): Array[Byte] = RowWriter.toBytes(data)

  override def close(): Unit = {
    // do nothing
  }
}
