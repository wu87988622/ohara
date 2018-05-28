package com.island.ohara.kafka

import java.util

import com.island.ohara.serialization.Serializer

/**
  * Make the wrap of kafka components.
  */
object KafkaUtil {

  /**
    * Used to convert ohara row to byte array. It is a private class since ohara producer will instantiate one and pass it to
    * kafka producer. Hence, no dynamical call will happen in kafka producer. The access exception won't be caused.
    * @param serializer ohara serializer
    * @tparam T object type
    * @return a wrapper of kafka serializer
    */
  def wrapSerializer[T](serializer: Serializer[T]) = new org.apache.kafka.common.serialization.Serializer[T]() {
    override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {
      // do nothing
    }

    override def serialize(topic: String, data: T): Array[Byte] = if (data == null) null else serializer.to(data)

    override def close(): Unit = {
      // do nothing
    }
  }

  /**
    * Used to convert byte array to ohara row. It is a private class since ohara consumer will instantiate one and pass it to
    * kafka consumer. Hence, no dynamical call will happen in kafka consumer. The access exception won't be caused.
    * @param serializer ohara serializer
    * @tparam T object type
    * @return a wrapper of kafka deserializer
    */
  def wrapDeserializer[T](serializer: Serializer[T]) = new org.apache.kafka.common.serialization.Deserializer[T]() {
    override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {
      // do nothing
    }

    override def deserialize(topic: String, data: Array[Byte]): T =
      if (data == null) null.asInstanceOf[T] else serializer.from(data)

    override def close(): Unit = {
      // do nothing
    }
  }
}
