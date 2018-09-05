package com.island.ohara.kafka

import java.util

import com.island.ohara.io.CloseOnce._
import com.island.ohara.serialization.Serializer
import org.apache.kafka.common.serialization
import org.apache.kafka.common.serialization.Deserializer

import scala.concurrent.duration._

/**
  * Make the wrap of kafka components.
  */
object KafkaUtil {
  private[this] val DEFAULT_TIMEOUT = 10 seconds

  /**
    * Used to convert ohara row to byte array. It is a private class since ohara producer will instantiate one and pass it to
    * kafka producer. Hence, no dynamical call will happen in kafka producer. The access exception won't be caused.
    *
    * @param serializer ohara serializer
    * @tparam T object type
    * @return a wrapper of kafka serializer
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
    * @return a wrapper of kafka deserializer
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
    * @param brokers the location of kafka brokers
    * @param topicName topic nameHDFSStorage
    * @return true if the topic exist. Otherwise, false
    */
  def exist(brokers: String, topicName: String, timeout: Duration = DEFAULT_TIMEOUT): Boolean =
    doClose(KafkaClient(brokers))(_.exist(topicName, timeout))

  def topicInfo(brokers: String, topicName: String, timeout: Duration = DEFAULT_TIMEOUT): Option[TopicDescription] =
    doClose(KafkaClient(brokers))(_.topicInfo(topicName, timeout))

  /**
    * Increate the number of partitions. This method check the number before doing the alter. If the number is equal
    * to the previous setting, nothing will happen; Decreasing the number is not allowed and it will cause
    * an IllegalArgumentException. Otherwise, this method use kafka AdminClient to send the request to increase the
    * number of partitions.
    *
    * @param brokers brokers information
    * @param topicName topic name
    * @param numberOfPartitions if this number is s
    */
  def addPartitions(brokers: String,
                    topicName: String,
                    numberOfPartitions: Int,
                    timeout: Duration = DEFAULT_TIMEOUT): Unit =
    doClose(KafkaClient(brokers))(_.addPartition(topicName, numberOfPartitions, timeout))

  def createTopic(brokers: String,
                  topicName: String,
                  numberOfPartitions: Int,
                  numberOfReplications: Short,
                  timeout: Duration = DEFAULT_TIMEOUT): Unit = doClose(KafkaClient(brokers)) { client =>
    client.topicCreator
      .timeout(timeout)
      .numberOfPartitions(numberOfPartitions)
      .numberOfReplications(numberOfReplications)
      .name(topicName)
      .build()
  }

}
