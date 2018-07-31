package com.island.ohara.kafka

import java.util
import java.util.Properties
import java.util.concurrent.TimeUnit

import com.island.ohara.io.CloseOnce
import com.island.ohara.serialization.Serializer
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.admin.{AdminClient, NewPartitions, NewTopic}
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException

import scala.concurrent.ExecutionException
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
    * @param brokers the location of kafka brokers
    * @param topicName topic nameHDFSStorage
    * @return true if the topic exist. Otherwise, false
    */
  def exist(brokers: String, topicName: String, timeout: Duration): Boolean =
    CloseOnce.doClose(AdminClient.create(toAdminProps(brokers)))(exist(_, topicName, timeout))

  /**
    * check whether the specified topic exist
    * @param admin kafka admin
    * @param topicName topic name
    * @return true if the topic exist. Otherwise, false
    */
  def exist(admin: AdminClient, topicName: String, timeout: Duration): Boolean =
    admin.listTopics().names().thenApply(_.contains(topicName)).get(timeout.toMillis, TimeUnit.MILLISECONDS)

  def topicInfo(brokers: String, topicName: String, timeout: Duration): Option[TopicInfo] =
    CloseOnce.doClose(AdminClient.create(toAdminProps(brokers)))(topicInfo(_, topicName, timeout))

  def topicInfo(admin: AdminClient, topicName: String, timeout: Duration): Option[TopicInfo] =
    try Option(admin.describeTopics(util.Arrays.asList(topicName)).values().get(topicName))
      .map(_.get(timeout.toMillis, TimeUnit.MILLISECONDS))
      .map(topicPartitionInfo =>
        TopicInfo(
          topicPartitionInfo.name(),
          topicPartitionInfo.partitions().size(),
          // TODO: seems it has chance that each partition has different number of replications. by chia
          topicPartitionInfo.partitions().get(0).replicas().size().toShort
      ))
    catch {
      case e: ExecutionException =>
        e.getCause match {
          case ee: UnknownTopicOrPartitionException => throw new IllegalArgumentException(ee.getMessage)
          case other: Throwable                     => throw other
        }
    }

  def topicCreator: TopicCreator = new TopicCreator()

  /**
    * a helper method used to put the brokers information to java properties.
    * Usually controlling the admin client only require the broker information.
    * @param brokers kafka brokers information
    * @return a properties with brokers information
    */
  def toAdminProps(brokers: String): Properties = {
    val adminProps = new Properties()
    adminProps.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokers)
    adminProps
  }

  /**
    * Increate the number of partitions. This method check the number before doing the alter. If the number is equal
    * to the previous setting, nothing will happen; Decreasing the number is not allowed and it will cause
    * an IllegalArgumentException. Otherwise, this method use kafka AdminClient to send the request to increase the
    * number of partitions.
    * @param brokers brokers information
    * @param topicName topic name
    * @param numberOfPartitions if this number is s
    */
  def addPartitions(brokers: String, topicName: String, numberOfPartitions: Int, timeout: Duration): Unit =
    CloseOnce.doClose(AdminClient.create(toAdminProps(brokers)))(
      addPartitions(_, topicName, numberOfPartitions, timeout))

  /**
    * Increate the number of partitions. This method check the number before doing the alter. If the number is equal
    * to the previous setting, nothing will happen; Decreasing the number is not allowed and it will cause
    * an IllegalArgumentException. Otherwise, this method use kafka AdminClient to send the request to increase the
    * number of partitions.
    * @param admin admin
    * @param topicName topic name
    * @param numberOfPartitions if this number is s
    */
  def addPartitions(admin: AdminClient, topicName: String, numberOfPartitions: Int, timeout: Duration): Unit = {
    if (!topicInfo(admin, topicName, timeout)
          .map(
            current =>
              if (current.partitions > numberOfPartitions)
                throw new IllegalArgumentException("Reducing the number of partitions is disallowed")
              else if (current.partitions == numberOfPartitions) false
              else true)
          .map(shouldRun => {
            if (shouldRun) {
              import scala.collection.JavaConverters._
              admin
                .createPartitions(Map(topicName -> NewPartitions.increaseTo(numberOfPartitions)).asJava)
                .all()
                .get(timeout.toMillis, TimeUnit.MILLISECONDS)
            }
            // this true means the topic exists
            true
          })
          .getOrElse(false)) throw new IllegalArgumentException(s"the topic:${topicName} isn't existed")
  }
}

case class TopicInfo(name: String, partitions: Int, replications: Short)

/**
  * a helper class used to create the kafka topic.
  * all member are protected since we have to implement a do-nothing TopicCreator in testing.
  */
class TopicCreator {
  protected var admin: Option[AdminClient] = None
  protected var brokers: Option[String] = None
  protected var topicName: Option[String] = None
  protected var numberOfPartitions: Option[Int] = None
  protected var numberOfReplications: Option[Short] = None
  protected var topicOptions: Option[Map[String, String]] = Some(Map.empty[String, String])
  protected var timeout: Option[Duration] = Some(10 seconds)

  def admin(admin: AdminClient): TopicCreator = {
    this.admin = Some(admin)
    this
  }

  def brokers(brokers: String): TopicCreator = {
    this.brokers = Some(brokers)
    this
  }

  def topicName(topicName: String): TopicCreator = {
    this.topicName = Some(topicName)
    this
  }

  def numberOfPartitions(partitions: Int): TopicCreator = {
    this.numberOfPartitions = Some(partitions)
    this
  }

  def numberOfReplications(replication: Short): TopicCreator = {
    this.numberOfReplications = Some(replication)
    this
  }

  def topicOptions(topicOptions: Map[String, String]): TopicCreator = {
    this.topicOptions = Some(topicOptions)
    this
  }

  def timeout(timeout: Duration): TopicCreator = {
    this.timeout = Some(timeout)
    this
  }

  private[this] def getOrCreateAdmin(): (AdminClient, Boolean) =
    admin.map((_, false)).getOrElse((AdminClient.create(KafkaUtil.toAdminProps(brokers.get)), true))

  def create(): Unit = {
    import scala.collection.JavaConverters._
    val (adminClient, needClose) = getOrCreateAdmin()
    try {
      if (!KafkaUtil.exist(adminClient, topicName.get, timeout.get)) {
        adminClient
          .createTopics(util.Arrays.asList(new NewTopic(topicName.get, numberOfPartitions.get, numberOfReplications.get)
            .configs(topicOptions.get.asJava)))
          .values()
          .get(topicName.get)
          .get(timeout.get.toMillis, TimeUnit.MILLISECONDS)
      }
    } finally if (needClose) adminClient.close()
  }
}
