package com.island.ohara.kafka

import java.util
import java.util.Properties
import java.util.concurrent.TimeUnit

import com.island.ohara.io.CloseOnce
import com.island.ohara.kafka.KafkaClient._
import com.island.ohara.serialization.Serializer
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.admin.{AdminClient, NewPartitions, NewTopic}
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException

import scala.concurrent.ExecutionException
import scala.concurrent.duration._

/**
  * a helper methods used by configurator. It provide many helper method to operate kafka cluster.
  */
trait KafkaClient extends CloseOnce {

  def topicCreator(): TopicCreator

  def exist(topicName: String, timeout: Duration = DEFAULT_TIMEOUT): Boolean

  def topicInfo(topicName: String, timeout: Duration = DEFAULT_TIMEOUT): Option[TopicDescription]

  def addPartition(topicName: String, numberOfPartitions: Int, timeout: Duration = DEFAULT_TIMEOUT): Unit

  def deleteTopic(topicName: String, timeout: Duration = DEFAULT_TIMEOUT): Unit

  def listTopics(timeout: Duration = DEFAULT_TIMEOUT): Seq[String]

  def brokers: String

  def consumerBuilder[K, V](keySerializer: Serializer[K], valueSerializer: Serializer[V]): ConsumerBuilder[K, V]
}

object KafkaClient {
  private val DEFAULT_TIMEOUT = 10 seconds

  /**
    * this impl will host a kafka.AdminClient so you must call the #close() to release the kafka.AdminClient.
    *
    * @param _brokers the kafka brokers information
    * @return a impl of KafkaClient
    */
  def apply(_brokers: String): KafkaClient = new KafkaClient() {
    private[this] val admin = AdminClient.create(toAdminProps(_brokers))

    override def exist(topicName: String, timeout: Duration): Boolean =
      admin.listTopics().names().thenApply(_.contains(topicName)).get(timeout.toMillis, TimeUnit.MILLISECONDS)

    override protected def doClose(): Unit = admin.close()

    import scala.collection.JavaConverters._
    override def topicCreator: TopicCreator = request => {
      admin
        .createTopics(
          util.Arrays.asList(new NewTopic(request.name, request.numberOfPartitions, request.numberOfReplications)
            .configs(request.options.asJava)))
        .values()
        .get(request.name)
        .get(request.timeout.toMillis, TimeUnit.MILLISECONDS)
    }

    override def topicInfo(topicName: String, timeout: Duration): Option[TopicDescription] =
      try Option(admin.describeTopics(util.Arrays.asList(topicName)).values().get(topicName))
        .map(_.get(timeout.toMillis, TimeUnit.MILLISECONDS))
        .map(topicPartitionInfo =>
          TopicDescription(
            topicPartitionInfo.name(),
            topicPartitionInfo.partitions().size(),
            // TODO: seems it has chance that each partition has different number of replications. by chia
            topicPartitionInfo.partitions().get(0).replicas().size().toShort
        ))
      catch {
        case e: ExecutionException =>
          e.getCause match {
            // substitute None for UnknownTopicOrPartitionException
            case _: UnknownTopicOrPartitionException => None
            case other: Throwable                    => throw other
          }
      }

    override def addPartition(topicName: String, numberOfPartitions: Int, timeout: Duration): Unit = {
      val current = topicInfo(topicName, timeout).getOrElse(
        throw new IllegalArgumentException(s"the topic:$topicName isn't existed"))
      if (current.numberOfPartitions > numberOfPartitions)
        throw new IllegalArgumentException("Reducing the number of partitions is disallowed")
      if (current.numberOfPartitions < numberOfPartitions) {
        import scala.collection.JavaConverters._
        admin
          .createPartitions(Map(topicName -> NewPartitions.increaseTo(numberOfPartitions)).asJava)
          .all()
          .get(timeout.toMillis, TimeUnit.MILLISECONDS)
      }
    }

    override def deleteTopic(topicName: String, timeout: Duration): Unit =
      admin.deleteTopics(util.Arrays.asList(topicName)).all().get(timeout.toMillis, TimeUnit.MILLISECONDS)

    override def listTopics(timeout: Duration): Seq[String] =
      admin.listTopics().names().get(timeout.toMillis, TimeUnit.MILLISECONDS).asScala.toList
    override def brokers: String = _brokers
    override def consumerBuilder[K, V](keySerializer: Serializer[K],
                                       valueSerializer: Serializer[V]): ConsumerBuilder[K, V] =
      new ConsumerBuilder[K, V](keySerializer, valueSerializer).brokers(brokers)
  }

  /**
    * a helper method used to put the brokers information to java properties.
    * Usually controlling the admin client only require the broker information.
    *
    * @param brokers kafka brokers information
    * @return a properties with brokers information
    */
  private[this] def toAdminProps(brokers: String): Properties = {
    val adminProps = new Properties()
    adminProps.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokers)
    adminProps
  }
}

case class TopicDescription(name: String, numberOfPartitions: Int, numberOfReplications: Short)
