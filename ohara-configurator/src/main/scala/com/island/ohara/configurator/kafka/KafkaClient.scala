package com.island.ohara.configurator.kafka

import java.util.concurrent.ConcurrentHashMap

import com.island.ohara.io.CloseOnce
import com.island.ohara.kafka.{KafkaUtil, TopicCreator, TopicInfo}
import com.typesafe.scalalogging.Logger
import org.apache.kafka.clients.admin.AdminClient

/**
  * a helper methods used by configurator. It provide many helper method to operate kafka cluster.
  */
trait KafkaClient extends CloseOnce {

  def topicCreator: TopicCreator

  def exist(topicName: String): Boolean

  def topicInfo(topicName: String): Option[TopicInfo]

  def addPartition(topicName: String, numberOfPartitions: Int): Unit
}

object KafkaClient {
  import scala.concurrent.duration._
  private[this] val DEFAULT_TIMEOUT = 15 seconds
  private[this] val LOG = Logger(KafkaClient.getClass.getName)

  /**
    * A do-nothing impl of KafkaClient.
    * NOTED: It should be used in testing only.
    */
  lazy val EMPTY = new KafkaClient() {
    private[this] val cachedTopics = new ConcurrentHashMap[String, TopicInfo]()
    override def exist(topicName: String): Boolean = {
      printDebugMessage()
      cachedTopics.contains(topicName)
    }
    override protected def doClose(): Unit = {
      printDebugMessage()
    }

    override def topicCreator: TopicCreator = new TopicCreator() {
      override def create(): Unit = {
        printDebugMessage()
        cachedTopics.put(topicName.get, TopicInfo(topicName.get, numberOfPartitions.get, numberOfReplications.get))
      }
    }

    override def addPartition(topicName: String, numberOfPartitions: Int): Unit = {
      printDebugMessage()
      Option(cachedTopics.get(topicName))
        .map(previous => TopicInfo(topicName, numberOfPartitions, previous.replications))
        .getOrElse(throw new IllegalArgumentException(s"the topic:$topicName doesn't exist"))
    }

    private[this] def printDebugMessage(): Unit =
      LOG.debug("You are using a empty kafka client!!! Please make sure this message only appear in testing")

    override def topicInfo(topicName: String): Option[TopicInfo] = Option(cachedTopics.get(topicName))
  }

  /**
    * this impl will host a kafka.AdminClient so you must call the #close() to release the kafka.AdminClient.
    * @param brokers the kafka brokers information
    * @return a impl of KafkaClient
    */
  def apply(brokers: String): KafkaClient = new KafkaClient() {
    private[this] val admin = AdminClient.create(KafkaUtil.toAdminProps(brokers))

    override def exist(topicName: String): Boolean = KafkaUtil.exist(admin, topicName, DEFAULT_TIMEOUT)

    override protected def doClose(): Unit = admin.close()

    override def topicCreator: TopicCreator = KafkaUtil.topicCreator.admin(admin)

    override def topicInfo(topicName: String): Option[TopicInfo] =
      KafkaUtil.topicInfo(admin, topicName, DEFAULT_TIMEOUT)

    override def addPartition(topicName: String, numberOfPartitions: Int): Unit =
      KafkaUtil.addPartitions(admin, topicName, numberOfPartitions, DEFAULT_TIMEOUT)
  }
}
