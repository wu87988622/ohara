package com.island.ohara.kafka

import java.util

import com.island.ohara.common.data.Serializer
import org.apache.kafka.common.serialization
import org.apache.kafka.common.serialization.Deserializer

import scala.concurrent.duration._

/**
  * Make the wrap from kafka components.
  */
object KafkaUtil {
  private[this] val DEFAULT_TIMEOUT = 10 seconds

  /**
    * Used to convert ohara row to byte array. It is a private class since ohara producer will instantiate one and pass it to
    * kafka producer. Hence, no dynamical call will happen in kafka producer. The access exception won't be caused.
    *
    * @param serializer ohara serializer
    * @tparam T object type
    * @return a wrapper from kafka serializer
    */
  def wrapSerializer[T](serializer: Serializer[T]): serialization.Serializer[T] =
    new org.apache.kafka.common.serialization.Serializer[T]() {
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
    *
    * @param serializer ohara serializer
    * @tparam T object type
    * @return a wrapper from kafka deserializer
    */
  def wrapDeserializer[T](serializer: Serializer[T]): Deserializer[T] =
    new org.apache.kafka.common.serialization.Deserializer[T]() {
      override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {
        // do nothing
      }

      override def deserialize(topic: String, data: Array[Byte]): T =
        if (data == null) null.asInstanceOf[T] else serializer.from(data)

      override def close(): Unit = {
        // do nothing
      }
    }

  /**
    * check whether the specified topic exist
    *
    * @param brokersConnProps the location from kafka brokersConnProps
    * @param topicName topic nameHDFSStorage
    * @return true if the topic exist. Otherwise, false
    */
  def exist(brokersConnProps: String, topicName: String, timeout: Duration = DEFAULT_TIMEOUT): Boolean = {
    val client = KafkaClient(brokersConnProps)
    try client.exist(topicName, timeout)
    finally client.close()
  }

  def topicDescription(brokersConnProps: String,
                       topicName: String,
                       timeout: Duration = DEFAULT_TIMEOUT): TopicDescription = {
    val client = KafkaClient(brokersConnProps)
    try client.topicDescription(topicName, timeout)
    finally client.close()
  }

  /**
    * Increate the number from partitions. This method check the number before doing the alter. If the number is equal
    * to the previous setting, nothing will happen; Decreasing the number is not allowed and it will cause
    * an IllegalArgumentException. Otherwise, this method use kafka AdminClient to send the request to increase the
    * number from partitions.
    *
    * @param brokersConnProps brokersConnProps information
    * @param topicName topic name
    * @param numberOfPartitions if this number is s
    */
  def addPartitions(brokersConnProps: String,
                    topicName: String,
                    numberOfPartitions: Int,
                    timeout: Duration = DEFAULT_TIMEOUT): Unit = {
    val client = KafkaClient(brokersConnProps)
    try client.addPartitions(topicName, numberOfPartitions, timeout)
    finally client.close()
  }

  def createTopic(brokersConnProps: String,
                  topicName: String,
                  numberOfPartitions: Int,
                  numberOfReplications: Short,
                  options: Map[String, String] = Map.empty,
                  timeout: Duration = DEFAULT_TIMEOUT): Unit = {
    val client = KafkaClient(brokersConnProps)
    try client
      .topicCreator()
      .timeout(timeout)
      .numberOfPartitions(numberOfPartitions)
      .numberOfReplications(numberOfReplications)
      .options(options)
      .create(topicName)
    finally client.close()
  }

  def deleteTopic(brokersConnProps: String, topicName: String, timeout: Duration = DEFAULT_TIMEOUT): Unit = {
    val client = KafkaClient(brokersConnProps)
    try client.deleteTopic(topicName, timeout)
    finally client.close()
  }
}
