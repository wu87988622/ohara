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
import com.island.ohara.common.util.{CommonUtils, Releasable}
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.admin.{AdminClient, NewPartitions, NewTopic}
import org.apache.kafka.common.config.TopicConfig

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

/**
  * this is a wrap of kafka's AdminClient. However, we only wrap the functions about "topic" since the others are useless
  * to us.
  */
trait TopicAdmin extends Releasable {

  /**
    * Change the number of partitions of topic.
    * Currently, reducing the number of partitions is not allowed!
    * @param name topic name
    * @param numberOfPartitions the partitions that given topic should have
    * @return topic information
    */
  def changePartitions(name: String, numberOfPartitions: Int)(
    implicit executionContext: ExecutionContext): Future[TopicAdmin.TopicInfo]

  /**
    * list all topics
    * @return topics information
    */
  def list(implicit executionContext: ExecutionContext): Future[Seq[TopicAdmin.TopicInfo]]

  /**
    * start a process to create topic
    * @return topic creator
    */
  def creator(): TopicAdmin.Creator

  /**
    * delete a existent topic
    * @param name topic name
    * @return topic information
    */
  def delete(name: String)(implicit executionContext: ExecutionContext): Future[TopicAdmin.TopicInfo]

  /**
    * the connection information to kafka's broker
    * @return connection props
    */
  def connectionProps: String

  def closed(): Boolean
}

object TopicAdmin {

  def apply(_connectionProps: String): TopicAdmin = new TopicAdmin {
    private[this] val _closed = new AtomicBoolean(false)
    override val connectionProps: String = _connectionProps

    override def closed(): Boolean = _closed.get()

    /**
      * extract the exception wrapped in ExecutionException.
      * @param f action
      * @tparam T return type
      * @return return value
      */
    private[this] def unwrap[T](f: () => T): T = try f()
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

    override def creator(): Creator =
      (executionContext,
       name: String,
       numberOfPartitions: Int,
       numberOfReplications: Short,
       cleanupPolicy: CleanupPolicy) =>
        Future {
          unwrap(
            () =>
              admin
                .createTopics(
                  util.Arrays.asList(new NewTopic(name, numberOfPartitions, numberOfReplications).configs(Map(
                    TopicConfig.CLEANUP_POLICY_CONFIG -> (cleanupPolicy match {
                      case CleanupPolicy.COMPACTED => TopicConfig.CLEANUP_POLICY_COMPACT
                      case _                       => TopicConfig.CLEANUP_POLICY_DELETE
                    })
                  ).asJava)))
                .values()
                .get(name)
                .get())
          TopicAdmin.TopicInfo(
            name = name,
            numberOfPartitions = numberOfPartitions,
            numberOfReplications = numberOfReplications
          )
        }(executionContext)

    /**
      * list name of topics
      * @return a list of topic's names
      */
    private[this] def listNames(implicit executionContext: ExecutionContext): Future[Seq[String]] = Future {
      unwrap(() => admin.listTopics().names().get().asScala.toSeq)
    }

    private[this] def list(names: Seq[String])(implicit executionContext: ExecutionContext): Future[Seq[TopicInfo]] =
      Future {
        unwrap(
          () =>
            admin
              .describeTopics(names.asJava)
              .all()
              .get()
              .values()
              .asScala
              .map(t =>
                TopicInfo(t.name(), t.partitions().size(), t.partitions().get(0).replicas().size().asInstanceOf[Short]))
              .toSeq)
      }

    override def list(implicit executionContext: ExecutionContext): Future[Seq[TopicInfo]] = listNames.flatMap(list)

    override def changePartitions(name: String, numberOfPartitions: Int)(
      implicit executionContext: ExecutionContext): Future[TopicInfo] = list.flatMap(
      _.find(_.name == name)
        .map { topicInfo =>
          if (topicInfo.numberOfPartitions > numberOfPartitions)
            Future.failed(new IllegalArgumentException("Reducing the number from partitions is disallowed"))
          else if (topicInfo.numberOfPartitions == numberOfPartitions) Future.successful(topicInfo)
          else
            Future {
              unwrap(
                () =>
                  admin
                    .createPartitions(Collections.singletonMap(name, NewPartitions.increaseTo(numberOfPartitions)))
                    .all()
                    .get())
            }.map(_ => topicInfo.copy(numberOfPartitions = numberOfPartitions))
        }
        .getOrElse(Future.failed(new NoSuchElementException(s"$name doesn't exist"))))

    override def delete(name: String)(implicit executionContext: ExecutionContext): Future[TopicInfo] = list.flatMap {
      topics =>
        val topic = topics.find(_.name == name).get
        Future {
          unwrap(() => admin.deleteTopics(util.Arrays.asList(name)).all().get)
        }.map(_ => topic)
    }
  }

  final case class TopicInfo(name: String, numberOfPartitions: Int, numberOfReplications: Short)

  trait Creator {
    private[this] var name: String = _
    private[this] var numberOfPartitions: Int = 1
    private[this] var numberOfReplications: Short = 1
    private[this] var cleanupPolicy: CleanupPolicy = CleanupPolicy.DELETE

    def name(name: String): Creator.this.type = {
      this.name = Objects.requireNonNull(name)
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
      this.cleanupPolicy = cleanupPolicy
      this
    }

    def create(implicit executionContext: ExecutionContext): Future[TopicAdmin.TopicInfo] = doCreate(
      executionContext = Objects.requireNonNull(executionContext),
      name = Objects.requireNonNull(name),
      numberOfPartitions = CommonUtils.requirePositiveInt(numberOfPartitions),
      numberOfReplications = CommonUtils.requirePositiveShort(numberOfReplications),
      cleanupPolicy
    )

    protected def doCreate(executionContext: ExecutionContext,
                           name: String,
                           numberOfPartitions: Int,
                           numberOfReplications: Short,
                           cleanupPolicy: CleanupPolicy): Future[TopicAdmin.TopicInfo]
  }
}
