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

import java.net.HttpRetryException
import java.util.Objects
import java.util.concurrent.TimeUnit

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.island.ohara.client.HttpExecutor
import com.island.ohara.client.kafka.WorkerJson._
import com.island.ohara.common.annotations.Optional
import com.island.ohara.common.data.Column
import com.island.ohara.common.util.CommonUtil
import com.typesafe.scalalogging.Logger
import spray.json.DefaultJsonProtocol._

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random

/**
  * a helper class used to send the rest request to kafka worker.
  */
trait WorkerClient {

  /**
    * start a process to create source/sink connector
    * @return connector creator
    */
  def connectorCreator(): WorkerClient.Creator

  /**
    * delete a connector from worker cluster
    * @param name connector's name
    * @return async future containing nothing
    */
  def delete(name: String): Future[Unit]

  /**
    * pause a running connector
    * @param name connector's name
    * @return async future containing nothing
    */
  def pause(name: String): Future[Unit]

  /**
    * resume a paused connector
    * @param name connector's name
    * @return async future containing nothing
    */
  def resume(name: String): Future[Unit]

  /**
    * list available plugins
    * @return async future containing connector details
    */
  def plugins(): Future[Seq[Plugin]]

  /**
    * list available plugin's names
    * @return async future containing connector's names
    */
  def activeConnectors(): Future[Seq[String]]

  /**
    * @return worker's connection props
    */
  def connectionProps: String

  /**
    * @param name connector's name
    * @return status of connector
    */
  def status(name: String): Future[ConnectorInfo]

  /**
    * @param name connector's name
    * @return configuration of connector
    */
  def config(name: String): Future[ConnectorConfig]

  /**
    * @param name connector's name
    * @param id task's id
    * @return task status
    */
  def taskStatus(name: String, id: Int): Future[TaskStatus]

  /**
    * Check whether a connector name is used in creating connector (even if the connector fails to start, this method
    * still return true)
    * @param name connector name
    * @return true if connector exists
    */
  def exist(name: String): Future[Boolean] = activeConnectors().map(_.contains(name))

  def nonExist(name: String): Future[Boolean] = exist(name).map(!_)
}

object WorkerClient {
  private[this] val LOG = Logger(WorkerClient.getClass)
  def apply(_connectionProps: String): WorkerClient = {
    val workerList = _connectionProps.split(",")
    if (workerList.isEmpty) throw new IllegalArgumentException(s"Invalid workers:${_connectionProps}")
    new WorkerClient() with SprayJsonSupport {
      private[this] val workerAddress: String = workerList(Random.nextInt(workerList.size))

      /**
        * kafka worker has weakness of doing consistent operation so it is easy to encounter conflict error. Wrapping all operations with
        * retry can relax us... by chia
        * @param exec do request
        * @tparam T response type
        * @return response
        */
      private[this] def retry[T](exec: () => Future[T]): Future[T] = exec().recoverWith {
        case _: HttpRetryException =>
          TimeUnit.SECONDS.sleep(1)
          retry(exec)
      }

      override def connectorCreator(): Creator = request =>
        retry(
          () =>
            HttpExecutor.SINGLETON.post[CreateConnectorRequest, CreateConnectorResponse, Error](
              s"http://$workerAddress/connectors",
              request)
      )

      override def delete(name: String): Future[Unit] =
        retry(() => HttpExecutor.SINGLETON.delete[Error](s"http://$workerAddress/connectors/$name"))

      override def plugins(): Future[Seq[Plugin]] = retry(
        () => HttpExecutor.SINGLETON.get[Seq[Plugin], Error](s"http://$workerAddress/connector-plugins"))
      override def activeConnectors(): Future[Seq[String]] = retry(
        () => HttpExecutor.SINGLETON.get[Seq[String], Error](s"http://$workerAddress/connectors"))

      override def connectionProps: String = _connectionProps

      override def status(name: String): Future[ConnectorInfo] = retry(
        () => HttpExecutor.SINGLETON.get[ConnectorInfo, Error](s"http://$workerAddress/connectors/$name/status"))

      override def config(name: String): Future[ConnectorConfig] = retry(
        () => HttpExecutor.SINGLETON.get[ConnectorConfig, Error](s"http://$workerAddress/connectors/$name/config"))

      override def taskStatus(name: String, id: Int): Future[TaskStatus] = retry(
        () => HttpExecutor.SINGLETON.get[TaskStatus, Error](s"http://$workerAddress/connectors/$name/tasks/$id/status"))
      override def pause(name: String): Future[Unit] = retry(
        () => HttpExecutor.SINGLETON.put[Error](s"http://$workerAddress/connectors/$name/pause"))

      override def resume(name: String): Future[Unit] = retry(
        () => HttpExecutor.SINGLETON.put[Error](s"http://$workerAddress/connectors/$name/resume"))
    }
  }

  /**
    * a base class used to collect the config from source/sink connector when creating
    */
  abstract class Creator {
    private[this] var name: String = _
    private[this] var clzName: String = _
    private[this] var topicNames: Seq[String] = _
    private[this] var numberOfTasks: Int = 1
    private[this] var configs: Map[String, String] = Map.empty
    private[this] var _disableKeyConverter: Boolean = false
    private[this] var _disableValueConverter: Boolean = false
    private[this] var schema: Seq[Column] = Seq.empty

    /**
      * config the key converter be org.apache.kafka.connect.converters.ByteArrayConverter. It is useful if the data in topic
      * your connector want to take is byte array and is generated by kafka producer. For example, the source is RowProducer,
      * and the target is RowSinkConnector.
      *
      * @return this one
      */
    @Optional("default key converter is org.apache.kafka.connect.json.JsonConverter")
    def disableKeyConverter(): Creator = {
      this._disableKeyConverter = true
      this
    }

    /**
      * config the value converter be org.apache.kafka.connect.converters.ByteArrayConverter. It is useful if the data in topic
      * your connector want to take is byte array and is generated by kafka producer. For example, the source is RowProducer,
      * and the target is RowSinkConnector.
      *
      * @return this one
      */
    @Optional("default value converter is org.apache.kafka.connect.json.JsonConverter")
    def disableValueConverter(): Creator = {
      this._disableValueConverter = true
      this
    }

    /**
      * config the converter be org.apache.kafka.connect.converters.ByteArrayConverter. It is useful if the data in topic
      * your connector want to take is byte array and is generated by kafka producer. For example, the source is RowProducer,
      * and the target is RowSinkConnector.
      *
      * @return this one
      */
    @Optional("default key/value converter is org.apache.kafka.connect.json.JsonConverter")
    def disableConverter(): Creator = {
      this._disableKeyConverter = true
      this._disableValueConverter = true
      this
    }

    /**
      * set the connector name. It should be a unique name.
      *
      * @param name connector name
      * @return this one
      */
    def name(name: String): this.type = {
      this.name = Objects.requireNonNull(name)
      this
    }

    /**
      * set the connector class. The class must be loaded in class loader otherwise it will fail to create the connector.
      *
      * @param clzName connector class
      * @return this one
      */
    def connectorClass(clzName: String): this.type = {
      this.clzName = Objects.requireNonNull(clzName)
      this
    }

    /**
      * set the connector class. The class must be loaded in class loader otherwise it will fail to create the connector.
      *
      * @param clz connector class
      * @return this one
      */
    def connectorClass[T](clz: Class[T]): this.type = {
      this.clzName = Objects.requireNonNull(clz.getName)
      this
    }

    /**
      * set the topic in which you have interest.
      *
      * @param topicName topic
      * @return this one
      */
    def topic(topicName: String): this.type = {
      this.topicNames = Seq(Objects.requireNonNull(topicName))
      this
    }

    /**
      * the max number from sink task you want to create
      *
      * @param numberOfTasks max number from sink task
      * @return this one
      */
    @Optional("default is 1")
    def numberOfTasks(numberOfTasks: Int): this.type = {
      this.numberOfTasks = CommonUtil.requirePositiveInt(numberOfTasks)
      this
    }

    /**
      * extra config passed to sink connector. This config is optional.
      *
      * @param configs config
      * @return this one
      */
    @Optional("default is empty")
    def configs(configs: Map[String, String]): this.type = {
      this.configs = Objects.requireNonNull(configs)
      this
    }

    /**
      * set the schema
      * @param schema schema
      * @return this builder
      */
    @Optional("default is empty")
    def schema(schema: Seq[Column]): this.type = {
      this.schema = Objects.requireNonNull(schema)
      this
    }

    /**
      * set the topics in which you have interest.
      *
      * @param topicNames topics
      * @return this one
      */
    def topics(topicNames: Seq[String]): this.type = {
      this.topicNames = Objects.requireNonNull(topicNames)
      this
    }

    /**
      * send the request to create the sink connector.
      *
      * @return this one
      */
    def create(): Future[CreateConnectorResponse] = {
      Objects.requireNonNull(name)
      Objects.requireNonNull(clzName)
      Objects.requireNonNull(topicNames)
      if (topicNames.isEmpty) throw new IllegalArgumentException(s"You must specify 1+ topic names")
      CommonUtil.requirePositiveInt(numberOfTasks)
      val kafkaConfig = new mutable.HashMap[String, String]()
      kafkaConfig ++= configs
      kafkaConfig += ("connector.class" -> clzName)
      kafkaConfig += ("topics" -> topicNames.mkString(","))
      kafkaConfig += ("tasks.max" -> numberOfTasks.toString)
      import scala.collection.JavaConverters._
      if (schema != null && schema.nonEmpty) kafkaConfig += (Column.COLUMN_KEY -> Column.fromColumns(schema.asJava))
      if (_disableKeyConverter)
        kafkaConfig += ("key.converter" -> "org.apache.kafka.connect.converters.ByteArrayConverter")
      if (_disableValueConverter)
        kafkaConfig += ("value.converter" -> "org.apache.kafka.connect.converters.ByteArrayConverter")
      // NOTED: If configs.name exists, kafka will use it to replace the outside name.
      // for example: {"name":"abc", "configs":{"name":"c"}} is converted to map("name", "c")...
      // Hence, we have to filter out the name here...
      kafkaConfig.remove("name").foreach(v => LOG.error(s"(name, $v) is removed from configs"))
      send(CreateConnectorRequest(name, kafkaConfig.toMap))
    }

    /**
      * send the request to kafka worker
      *
      * @return response
      */
    protected def send(request: CreateConnectorRequest): Future[CreateConnectorResponse]
  }
}
