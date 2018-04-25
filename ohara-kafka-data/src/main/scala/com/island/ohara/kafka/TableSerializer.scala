package com.island.ohara.kafka

import java.util

import com.island.ohara.core.Table
import com.island.ohara.serialization.TableWriter
import org.apache.kafka.common.serialization.Serializer

/**
  * Used to convert ohara table to byte array. It is a private class since ohara producer will instantiate one and pass it to
  * kafka producer. Hence, no dynamical call will happen in kafka producer. The access exception won't be caused.
  */
private class TableSerializer extends Serializer[Table] {
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {
    // do nothing
  }

  override def serialize(topic: String, data: Table): Array[Byte] = TableWriter.toBytes(data)

  override def close(): Unit = {
    // do nothing
  }
}
