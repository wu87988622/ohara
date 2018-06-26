package com.island.ohara.kafka

import java.util
import java.util.Properties
import java.util.concurrent.TimeUnit

import com.island.ohara.config.OharaConfig
import com.island.ohara.io.CloseOnce
import com.island.ohara.serialization.Serializer
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.admin.{AdminClient, NewTopic}

import scala.concurrent.duration._

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

  /**
    * check whether the specified topic exist
    * @param config the config used to build the kafka admin
    * @param topicName topic nameHDFSStorage
    * @return true if the topic exist. Otherwise, false
    */
  def exist(config: OharaConfig, topicName: String): Boolean =
    CloseOnce.doClose(AdminClient.create(config.toProperties)) { admin =>
      exist(admin, topicName)
    }

  /**
    * check whether the specified topic exist
    * @param admin kafka admin
    * @param topicName topic name
    * @return true if the topic exist. Otherwise, false
    */
  def exist(admin: AdminClient, topicName: String): Boolean =
    admin.listTopics().names().thenApply(_.contains(topicName)).get()

  def createTopicIfNonexistent(brokers: String,
                               topicName: String,
                               partitions: Int,
                               replication: Short,
                               topicConfig: Map[String, String] = Map[String, String](),
                               timeout: Duration = 10 seconds): Unit = {
    import scala.collection.JavaConverters._
    val adminProps = new Properties()
    adminProps.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokers)
    CloseOnce.doClose(AdminClient.create(adminProps))(admin => {
      if (!exist(admin, topicName)) {
        admin
          .createTopics(
            util.Arrays.asList(new NewTopic(topicName, partitions, replication).configs(topicConfig.asJava)))
          .values()
          .get(topicName)
          .get(timeout.toMillis, TimeUnit.MILLISECONDS)
      }
    })
  }
}
