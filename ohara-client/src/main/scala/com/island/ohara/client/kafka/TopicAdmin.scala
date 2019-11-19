/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.island.ohara.client.kafka

import java.util
import java.util.concurrent.ExecutionException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.{Collections, Objects, Properties}

import com.island.ohara.common.annotations.Optional
import com.island.ohara.common.setting.TopicKey
import com.island.ohara.common.util.{CommonUtils, Releasable}
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.admin.{AdminClient, NewPartitions, NewTopic, TopicDescription}
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.config.{ConfigResource, TopicConfig}
import org.apache.kafka.common.serialization.ByteArrayDeserializer

import scala.collection.JavaConverters._
import scala.concurrent.{Future, Promise}

/**
  * this is a wrap of kafka's AdminClient. However, we only wrap the functions about "topic" since the others are useless
  * to us.
  */
trait TopicAdmin extends Releasable {
  /**
    * Change the number of partitions of topic.
    * Currently, reducing the number of partitions is not allowed!
    * @param topicKey topic key
    * @param numberOfPartitions the partitions that given topic should have
    * @return topic information
    */
  def changePartitions(topicKey: TopicKey, numberOfPartitions: Int): Future[Unit]

  /**
    * list all topics
    * @return topics information
    */
  def topics(): Future[Seq[TopicAdmin.KafkaTopicInfo]]

  /**
    * check the existence of topic on remote broker cluster
    * @param topicKey topic key
    * @return true if the topic lies in the cluster. Otherwise, false
    */
  def exist(topicKey: TopicKey): Future[Boolean]

  /**
    * start a process to create topic
    * @return topic creator
    */
  def creator: TopicAdmin.Creator

  /**
    * delete a existent topic
    * @param topicKey topic key
    * @return true if it does delete a topic. otherwise, false
    */
  def delete(topicKey: TopicKey): Future[Boolean] = delete(topicKey.topicNameOnKafka())

  def delete(topicName: String): Future[Boolean]

  /**
    * the connection information to kafka's broker
    * @return connection props
    */
  def connectionProps: String

  def closed: Boolean
}

object TopicAdmin {
  def apply(_connectionProps: String): TopicAdmin = new TopicAdmin {
    private[this] val _closed            = new AtomicBoolean(false)
    override val connectionProps: String = _connectionProps

    override def closed: Boolean = _closed.get()

    /**
      * extract the exception wrapped in ExecutionException.
      * @param f action
      * @tparam T return type
      * @return return value
      */
    private[this] def unwrap[T](f: () => T): T =
      try f()
      catch {
        case e: ExecutionException =>
          throw e.getCause
      }
    private[this] def toAdminProps(connectionProps: String): Properties = {
      val adminProps = new Properties
      adminProps.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, Objects.requireNonNull(connectionProps))
      adminProps
    }

    private[this] val admin = AdminClient.create(toAdminProps(connectionProps))

    override def close(): Unit = if (_closed.compareAndSet(false, true)) Releasable.close(admin)

    override def creator: Creator =
      (topicKey: TopicKey, numberOfPartitions: Int, numberOfReplications: Short, configs: Map[String, String]) => {
        val promise = Promise[Unit]
        unwrap(
          () =>
            admin
              .createTopics(
                util.Collections.singletonList(
                  new NewTopic(topicKey.topicNameOnKafka(), numberOfPartitions, numberOfReplications)
                    .configs(configs.asJava)
                )
              )
              .values()
              .get(topicKey.topicNameOnKafka())
              .whenComplete((_, exception) => {
                if (exception == null)
                  promise.success(Unit)
                else promise.failure(exception)
              })
        )
        promise.future
      }
    type TopicName       = String
    type Partition       = Int
    type BeginningOffset = Long
    type EndOffset       = Long

    private[this] def toTopicInfo(
      topicDescription: TopicDescription,
      configs: Map[String, String],
      offsets: Map[TopicName, Map[Partition, (BeginningOffset, EndOffset)]]
    ): KafkaTopicInfo = {
      new KafkaTopicInfo(
        name = topicDescription.name(),
        numberOfPartitions = topicDescription.partitions().size(),
        numberOfReplications = topicDescription.partitions().get(0).replicas().size().asInstanceOf[Short],
        partitionInfos = topicDescription.partitions().asScala.map { kafkaPartition =>
          val (beginningOffset, endOffset) =
            offsets.get(topicDescription.name()).flatMap(ps => ps.get(kafkaPartition.partition())).getOrElse((-1L, -1L))
          new KafkaPartitionInfo(
            index = kafkaPartition.partition(),
            leaderNode = kafkaPartition.leader().host(),
            replicaNodes = kafkaPartition.replicas().asScala.map(_.host()).toSet,
            inSyncReplicaNodes = kafkaPartition.isr().asScala.map(_.host()).toSet,
            beginningOffset = beginningOffset,
            endOffset = endOffset
          )
        },
        configs = configs
      )
    }

    /**
      * fetch the low/high offsets from all topics.
      * the client code is KafkaConsumer than KafkaAdmin since the later is unsupported to fetch offsets ...
      */
    private[this] def topicOffsets(): Map[TopicName, Map[Partition, (BeginningOffset, EndOffset)]] = {
      val config = new Properties
      config.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, _connectionProps)
      config.setProperty(ConsumerConfig.CLIENT_ID_CONFIG, CommonUtils.randomString())
      val consumer =
        new KafkaConsumer[Array[Byte], Array[Byte]](config, new ByteArrayDeserializer, new ByteArrayDeserializer)
      try {
        consumer
          .listTopics()
          .asScala
          .map {
            case (topicName, pts) => topicName -> pts.asScala.map(p => new TopicPartition(p.topic(), p.partition()))
          }
          .map {
            case (topicName, pts) =>
              def fetchOffset(offsets: Map[TopicPartition, java.lang.Long], partition: TopicPartition): Long =
                offsets.getOrElse(
                  partition,
                  throw new NoSuchElementException(
                    s"topic:${partition.topic()} partition:${partition.partition()} does not exist"
                  )
                )
              val beginningOffsets = consumer.beginningOffsets(pts.asJava).asScala.toMap
              val endOffsets       = consumer.endOffsets(pts.asJava).asScala.toMap
              topicName -> pts
                .map(pt => pt.partition() -> (fetchOffset(beginningOffsets, pt) -> fetchOffset(endOffsets, pt)))
                .toMap
          }
          .toMap
      } finally Releasable.close(consumer)
    }

    override def topics(): Future[Seq[KafkaTopicInfo]] =
      try {
        val allOffsets = topicOffsets()
        val promise    = Promise[Seq[KafkaTopicInfo]]
        unwrap(
          () =>
            admin
              .listTopics()
              .names()
              .whenComplete((topicNames, exception) => {
                if (exception == null)
                  admin
                    .describeTopics(topicNames)
                    .all()
                    .whenComplete((topicDescriptions, exception) => {
                      if (exception == null)
                        admin
                          .describeConfigs(
                            topicDescriptions
                              .keySet()
                              .asScala
                              .map(name => new ConfigResource(ConfigResource.Type.TOPIC, name))
                              .asJava
                          )
                          .all()
                          .whenComplete(
                            (topicsAndConfigs, exception) =>
                              try {
                                if (exception == null)
                                  promise.success(
                                    topicDescriptions
                                      .values()
                                      .asScala
                                      .map { topicDescription =>
                                        val configs = topicsAndConfigs.asScala
                                          .find(_._1.name() == topicDescription.name())
                                          .map(_._2.entries().asScala.map(e => e.name() -> e.value()).toMap)
                                          .getOrElse(Map.empty)
                                        toTopicInfo(topicDescription, configs, allOffsets)
                                      }
                                      .toSeq
                                  )
                                else promise.failure(exception)
                              } catch {
                                case e: Throwable => promise.failure(e)
                              }
                          )
                      else promise.failure(exception)
                    })
                else promise.failure(exception)
              })
        )
        promise.future
      } catch {
        case e: Throwable => Future.failed(e)
      }

    override def changePartitions(topicKey: TopicKey, numberOfPartitions: Int): Future[Unit] = {
      val promise = Promise[Unit]
      unwrap(
        () =>
          admin
            .createPartitions(
              Collections.singletonMap(topicKey.topicNameOnKafka(), NewPartitions.increaseTo(numberOfPartitions))
            )
            .all()
            .whenComplete((_, exception) => {
              if (exception == null) promise.success(Unit)
              else promise.failure(exception)
            })
      )
      promise.future
    }

    override def delete(topicName: String): Future[Boolean] = {
      val promise = Promise[Boolean]
      unwrap(
        () =>
          admin
            .listTopics()
            .names()
            .whenComplete((topicNames, exception) => {
              if (exception == null) {
                if (topicNames.asScala.contains(topicName))
                  admin
                    .deleteTopics(util.Collections.singletonList(topicName))
                    .all()
                    .whenComplete((_, exception) => {
                      if (exception == null) promise.success(true)
                      else promise.failure(exception)
                    })
                else
                  promise.success(false)
              } else promise.failure(exception)
            })
      )
      promise.future
    }

    override def exist(topicKey: TopicKey): Future[Boolean] = {
      val promise = Promise[Boolean]
      unwrap(
        () =>
          admin
            .listTopics()
            .names()
            .whenComplete((topicNames, exception) => {
              if (exception == null) promise.success(topicNames.asScala.contains(topicKey.topicNameOnKafka()))
              else promise.failure(exception)
            })
      )
      promise.future
    }
  }

  final class KafkaPartitionInfo(
    val index: Int,
    val leaderNode: String,
    val replicaNodes: Set[String],
    val inSyncReplicaNodes: Set[String],
    val beginningOffset: Long,
    val endOffset: Long
  )

  final class KafkaTopicInfo(
    val name: String,
    val numberOfPartitions: Int,
    val numberOfReplications: Short,
    val partitionInfos: Seq[KafkaPartitionInfo],
    val configs: Map[String, String]
  )

  trait Creator extends com.island.ohara.common.pattern.Creator[Future[Unit]] {
    private[this] var topicKey: TopicKey          = _
    private[this] var numberOfPartitions: Int     = 1
    private[this] var numberOfReplications: Short = 1
    private[this] var configs: Map[String, String] = Map(
      TopicConfig.CLEANUP_POLICY_CONFIG -> CleanupPolicy.DELETE.name
    )
    def topicKey(topicKey: TopicKey): Creator.this.type = {
      this.topicKey = Objects.requireNonNull(topicKey)
      this
    }

    @Optional("default is 1")
    def numberOfPartitions(numberOfPartitions: Int): Creator = {
      this.numberOfPartitions = CommonUtils.requirePositiveInt(numberOfPartitions)
      this
    }

    @Optional("default is 1")
    def numberOfReplications(numberOfReplications: Short): Creator = {
      this.numberOfReplications = CommonUtils.requirePositiveShort(numberOfReplications)
      this
    }

    @Optional("default is CleanupPolicy.DELETE")
    def cleanupPolicy(cleanupPolicy: CleanupPolicy): Creator = {
      this.configs ++= Map(TopicConfig.CLEANUP_POLICY_CONFIG -> cleanupPolicy.name)
      this
    }

    def config(key: String, value: String): Creator = {
      this.configs ++= Map(CommonUtils.requireNonEmpty(key) -> CommonUtils.requireNonEmpty(value))
      this
    }

    def configs(configs: Map[String, String]): Creator = {
      this.configs ++= Objects.requireNonNull(configs)
      this
    }

    override def create(): Future[Unit] = doCreate(
      topicKey = Objects.requireNonNull(topicKey),
      numberOfPartitions = CommonUtils.requirePositiveInt(numberOfPartitions),
      numberOfReplications = CommonUtils.requirePositiveShort(numberOfReplications),
      configs = Objects.requireNonNull(configs)
    )

    protected def doCreate(
      topicKey: TopicKey,
      numberOfPartitions: Int,
      numberOfReplications: Short,
      configs: Map[String, String]
    ): Future[Unit]
  }
}
