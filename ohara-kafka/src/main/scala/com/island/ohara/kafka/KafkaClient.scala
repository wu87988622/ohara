package com.island.ohara.kafka

import java.util
import java.util.Properties
import java.util.concurrent.TimeUnit

import com.island.ohara.kafka.KafkaClient._
import com.island.ohara.io.CloseOnce
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.admin.{AdminClient, NewPartitions, NewTopic}
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException

import scala.concurrent.ExecutionException
import scala.concurrent.duration._

/**
  * a helper methods used by configurator. It provide many helper method to operate kafka cluster.
  */
trait KafkaClient extends CloseOnce {

  def topicCreator: TopicCreator

  def exist(topicName: String, timeout: Duration = DEFAULT_TIMEOUT): Boolean

  def topicInfo(topicName: String, timeout: Duration = DEFAULT_TIMEOUT): Option[TopicDescription]

  def addPartition(topicName: String, numberOfPartitions: Int, timeout: Duration = DEFAULT_TIMEOUT): Unit

  def deleteTopic(topicName: String, timeout: Duration = DEFAULT_TIMEOUT): Unit

  def listTopics(timeout: Duration = DEFAULT_TIMEOUT): Seq[String]

  def brokersString: String
}

object KafkaClient {
  private val DEFAULT_TIMEOUT = 10 seconds

  /**
    * this impl will host a kafka.AdminClient so you must call the #close() to release the kafka.AdminClient.
    *
    * @param brokers the kafka brokers information
    * @return a impl of KafkaClient
    */
  def apply(brokers: String): KafkaClient = new KafkaClient() {
    private[this] val admin = AdminClient.create(toAdminProps(brokers))

    override def exist(topicName: String, timeout: Duration): Boolean =
      admin.listTopics().names().thenApply(_.contains(topicName)).get(timeout.toMillis, TimeUnit.MILLISECONDS)

    override protected def doClose(): Unit = admin.close()

    import scala.collection.JavaConverters._
    override def topicCreator: TopicCreator = new TopicCreator() {
      override def create(): Unit = if (!exist(topicName.get)) {
        admin
          .createTopics(util.Arrays.asList(new NewTopic(topicName.get, numberOfPartitions.get, numberOfReplications.get)
            .configs(topicOptions.get.asJava)))
          .values()
          .get(topicName.get)
          .get(timeout.get.toMillis, TimeUnit.MILLISECONDS)
      }
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

    override def addPartition(topicName: String, numberOfPartitions: Int, timeout: Duration): Unit =
      if (!topicInfo(topicName, timeout)
            .map(
              current =>
                if (current.numberOfPartitions > numberOfPartitions)
                  throw new IllegalArgumentException("Reducing the number of partitions is disallowed")
                else if (current.numberOfPartitions == numberOfPartitions) false
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

    override def deleteTopic(topicName: String, timeout: Duration): Unit =
      admin.deleteTopics(util.Arrays.asList(topicName)).all().get(timeout.toMillis, TimeUnit.MILLISECONDS)

    override def listTopics(timeout: Duration): Seq[String] =
      admin.listTopics().names().get(timeout.toMillis, TimeUnit.MILLISECONDS).asScala.toList
    override def brokersString: String = brokers
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
