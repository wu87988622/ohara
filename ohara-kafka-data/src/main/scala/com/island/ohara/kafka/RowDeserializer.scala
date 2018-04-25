package com.island.ohara.kafka

import java.util

import com.island.ohara.core.Row
import com.island.ohara.serialization.RowReader
import org.apache.kafka.common.serialization.Deserializer

/**
  * Used to convert byte array to ohara row. It is a private class since ohara consumer will instantiate one and pass it to
  * kafka consumer. Hence, no dynamical call will happen in kafka consumer. The access exception won't be caused.
  */
class RowDeserializer extends Deserializer[Row] {
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {
    // do nothing
  }

  override def deserialize(topic: String, data: Array[Byte]): Row = RowReader.toRow(data)

  override def close(): Unit = {
    // do nothing
  }
}
