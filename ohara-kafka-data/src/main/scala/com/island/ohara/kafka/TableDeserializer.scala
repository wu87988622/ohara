package com.island.ohara.kafka

import java.util

import com.island.ohara.core.Table
import com.island.ohara.serialization.TableReader
import org.apache.kafka.common.serialization.Deserializer

/**
  * Used to convert byte array to ohara table. It is a private class since ohara consumer will instantiate one and pass it to
  * kafka consumer. Hence, no dynamical call will happen in kafka consumer. The access exception won't be caused.
  */
private class TableDeserializer extends Deserializer[Table] {
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {
    // do nothing
  }

  override def deserialize(topic: String, data: Array[Byte]): Table = TableReader.toTable(data)

  override def close(): Unit = {
    // do nothing
  }
}
