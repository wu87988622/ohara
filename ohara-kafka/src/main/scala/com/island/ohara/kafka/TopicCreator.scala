package com.island.ohara.kafka
import org.apache.kafka.common.config.TopicConfig

import scala.concurrent.duration._

/**
  * a helper class used to create the kafka topic.
  * all member are protected since we have to implement a do-nothing TopicCreator in testing.
  */
abstract class TopicCreator {
  private[this] var numberOfPartitions: Int = 1
  private[this] var numberOfReplications: Short = 1
  private[this] var options: Map[String, String] = Map.empty
  private[this] var timeout: Duration = 10 seconds

  def numberOfPartitions(numberOfPartitions: Int): TopicCreator = {
    this.numberOfPartitions = numberOfPartitions
    this
  }

  def numberOfReplications(numberOfReplications: Short): TopicCreator = {
    this.numberOfReplications = numberOfReplications
    this
  }

  def options(options: Map[String, String]): TopicCreator = doOptions(options, true)

  /**
    * Specify that the topic's data should be compacted. It means the topic will keep the latest value for each key.
    * @return this builder
    */
  def compacted(): TopicCreator = {
    doOptions(Map(TopicConfig.CLEANUP_POLICY_CONFIG -> TopicConfig.CLEANUP_POLICY_COMPACT), false)
    this
  }

  /**
    * Specify that the topic's data should be deleted. It means the topic won't keep any data when cleanup
    * @return this builder
    */
  def deleted(): TopicCreator = {
    doOptions(Map(TopicConfig.CLEANUP_POLICY_CONFIG -> TopicConfig.CLEANUP_POLICY_DELETE), false)
    this
  }

  private[this] def doOptions(options: Map[String, String], overwrite: Boolean): TopicCreator = {
    if (this.options == null || overwrite) this.options = options
    else {
      this.options
        .filter {
          case (k, _) => options.contains(k)
        }
        .foreach {
          case (k, v) =>
            if (options(k) != v) throw new IllegalArgumentException(s"conflict options! previous:$v new:${options(k)}")
        }
      this.options ++= options
    }
    this
  }

  def timeout(timeout: Duration): TopicCreator = {
    this.timeout = timeout
    this
  }
  import TopicCreator._
  def create(name: String): Unit = doCreate(Request(name, numberOfPartitions, numberOfReplications, options, timeout))

  protected def doCreate(request: Request): Unit
}

object TopicCreator {
  case class Request(name: String,
                     numberOfPartitions: Int,
                     numberOfReplications: Short,
                     options: Map[String, String],
                     timeout: Duration)
}
