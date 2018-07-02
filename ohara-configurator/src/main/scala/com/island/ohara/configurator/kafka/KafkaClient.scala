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
}

object KafkaClient {
  private[this] val log = Logger(KafkaClient.getClass.getName)

  /**
    * A do-nothing impl of KafkaClient.
    * NOTED: It should be used in testing only.
    */
  val empty = new KafkaClient() {
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

    private[this] def printDebugMessage(): Unit =
      log.debug("You are using a empty kafka client!!! Please make sure this message only appear in testing")

    override def topicInfo(topicName: String): Option[TopicInfo] = Option(cachedTopics.get(topicName))
  }

  /**
    * this impl will host a kafka.AdminClient so you must call the #close() to release the kafka.AdminClient.
    * @param brokers the kafka brokers information
    * @return a impl of KafkaClient
    */
  def apply(brokers: String): KafkaClient = new KafkaClient() {
    private[this] val admin = AdminClient.create(KafkaUtil.toAdminProps(brokers))

    override def exist(topicName: String): Boolean = KafkaUtil.exist(admin, topicName)

    override protected def doClose(): Unit = admin.close()

    override def topicCreator: TopicCreator = KafkaUtil.topicCreator.admin(admin)

    override def topicInfo(topicName: String): Option[TopicInfo] = KafkaUtil.topicInfo(admin, topicName)
  }
}
