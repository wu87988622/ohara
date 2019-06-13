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
import akka.stream.StreamTcpException
import com.island.ohara.client.HttpExecutor
import com.island.ohara.client.configurator.v0.WorkerApi.ConnectorDefinitions
import com.island.ohara.client.kafka.WorkerJson._
import com.island.ohara.common.annotations.Optional
import com.island.ohara.common.data.Column
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.kafka.connector.json.{ConverterType, _}
import com.typesafe.scalalogging.Logger
import org.apache.kafka.connect.runtime.rest.entities.ConfigInfos
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
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
    * start a process to verify the connector
    * @return connector validator
    */
  def connectorValidator(): WorkerClient.Validator

  /**
    * delete a connector from worker cluster
    * @param name connector's name
    * @return async future containing nothing
    */
  def delete(name: String)(implicit executionContext: ExecutionContext): Future[Unit]

  /**
    * pause a running connector
    * @param name connector's name
    * @return async future containing nothing
    */
  def pause(name: String)(implicit executionContext: ExecutionContext): Future[Unit]

  /**
    * resume a paused connector
    * @param name connector's name
    * @return async future containing nothing
    */
  def resume(name: String)(implicit executionContext: ExecutionContext): Future[Unit]

  /**
    * list available plugins.
    * This main difference between plugins() and connectors() is that plugins() spend only one request
    * to remote server. In contrast, connectors() needs multi-requests to fetch all details from
    * remote server. Furthermore, connectors list only sub class from ohara's connectors
    * @return async future containing connector details
    */
  def plugins(implicit executionContext: ExecutionContext): Future[Seq[Plugin]]

  /**
    * list ohara's connector.
    * NOTED: the plugins which are not sub class of ohara connector are not included.
    * @return async future containing connector details
    */
  def connectors(implicit executionContext: ExecutionContext): Future[Seq[ConnectorDefinitions]]

  /**
    * list available plugin's names
    * @return async future containing connector's names
    */
  def activeConnectors(implicit executionContext: ExecutionContext): Future[Seq[String]]

  /**
    * @return worker's connection props
    */
  def connectionProps: String

  /**
    * @param name connector's name
    * @return status of connector
    */
  def status(name: String)(implicit executionContext: ExecutionContext): Future[ConnectorInfo]

  /**
    * a helper method which is composed by exist and status. It is useful when you want to fetch a may-be-nonexistent
    * connector but you hate to write future-chain code.
    * @param name connector's name
    * @param executionContext thread pool
    * @return status of nothing
    */
  def statusOrNone(name: String)(implicit executionContext: ExecutionContext): Future[Option[ConnectorInfo]] =
    exist(name).flatMap(if (_) status(name).map(Some(_)) else Future.successful(None))

  /**
    * @param name connector's name
    * @return configuration of connector
    */
  def config(name: String)(implicit executionContext: ExecutionContext): Future[ConnectorConfig]

  /**
    * @param name connector's name
    * @param id task's id
    * @return task status
    */
  def taskStatus(name: String, id: Int)(implicit executionContext: ExecutionContext): Future[TaskStatus]

  /**
    * Check whether a connector name is used in creating connector (even if the connector fails to start, this method
    * still return true)
    * @param name connector name
    * @return true if connector exists
    */
  def exist(name: String)(implicit executionContext: ExecutionContext): Future[Boolean] =
    activeConnectors.map(_.contains(name))

  def nonExist(name: String)(implicit executionContext: ExecutionContext): Future[Boolean] = exist(name).map(!_)

  /**
    * list all definitions for connector.
    * This is a helper method which passing "nothing" to validate the connector and then fetch only the definitions from report
    * @param connectorClassName class name
    * @param executionContext thread pool
    * @return definition list
    */
  def definitions(connectorClassName: String)(
    implicit executionContext: ExecutionContext): Future[Seq[SettingDefinition]] =
    connectorValidator()
      .className(connectorClassName)
      // kafka 2.x requires topic names for all sink connectors so we add a random topic for this request.
      .topicName(CommonUtils.randomString(5))
      .run()
      .map(_.settings().asScala.map(_.definition()))

}

object WorkerClient {
  private[this] val LOG = Logger(WorkerClient.getClass)

  /**
    * This is a bridge between java and scala.
    * ConfigInfos is serialized to json by jackson so we can implement the RootJsonFormat easily.
    */
  private[this] implicit val CONFIG_INFOS_JSON_FORMAT: RootJsonFormat[ConfigInfos] = new RootJsonFormat[ConfigInfos] {
    import spray.json._
    override def write(obj: ConfigInfos): JsValue = KafkaJsonUtils.toString(obj).parseJson

    override def read(json: JsValue): ConfigInfos = KafkaJsonUtils.toConfigInfos(json.toString())
  }

  /**
    * Create a default implementation of worker client.
    * NOTED: default implementation use a global akka system to handle http request/response. It means the connection
    * sent by this worker client may be influenced by other instances.
    * @param _connectionProps connection props
    * @param maxRetry times to retry
    * @return worker client
    */
  def apply(_connectionProps: String, maxRetry: Int = 3): WorkerClient = {
    val workerList = _connectionProps.split(",")
    if (workerList.isEmpty) throw new IllegalArgumentException(s"Invalid workers:${_connectionProps}")
    new WorkerClient() with SprayJsonSupport {
      private[this] def workerAddress: String = workerList(Random.nextInt(workerList.size))

      /**
        * kafka worker has weakness of doing consistent operation so it is easy to encounter conflict error. Wrapping all operations with
        * retry can relax us... by chia
        * @param exec do request
        * @tparam T response type
        * @return response
        */
      private[this] def retry[T](exec: () => Future[T], msg: String, retryCount: Int = 0)(
        implicit executionContext: ExecutionContext): Future[T] =
        exec().recoverWith {
          case e @ (_: HttpRetryException | _: StreamTcpException) =>
            LOG.info(s"$msg $retryCount/$maxRetry", e)
            if (retryCount < maxRetry) {
              TimeUnit.SECONDS.sleep(3)
              retry(exec, msg, retryCount + 1)
            } else throw e
        }

      override def connectorCreator(): Creator = (executionContext, creation) => {
        implicit val exec: ExecutionContext = executionContext
        retry(
          () =>
            HttpExecutor.SINGLETON.post[Creation, ConnectorCreationResponse, Error](
              s"http://$workerAddress/connectors",
              creation
          ),
          "connectorCreator"
        )
      }

      override def delete(name: String)(implicit executionContext: ExecutionContext): Future[Unit] =
        retry(() => HttpExecutor.SINGLETON.delete[Error](s"http://$workerAddress/connectors/$name"), s"delete $name")

      override def plugins(implicit executionContext: ExecutionContext): Future[Seq[Plugin]] = retry(
        () => HttpExecutor.SINGLETON.get[Seq[Plugin], Error](s"http://$workerAddress/connector-plugins"),
        s"fetch plugins $workerAddress")

      override def connectors(implicit executionContext: ExecutionContext): Future[Seq[ConnectorDefinitions]] =
        plugins
          .flatMap(Future.traverse(_) { p =>
            definitions(p.className)
              .map(
                definitions =>
                  ConnectorDefinitions(
                    className = p.className,
                    definitions = definitions
                ))
              .recover {
                // It should fail if we try to parse non-ohara connectors
                case _: IllegalArgumentException => ConnectorDefinitions(p.className, Seq.empty)
              }
          })
          .map(_.filter(_.definitions.nonEmpty))

      override def activeConnectors(implicit executionContext: ExecutionContext): Future[Seq[String]] = retry(
        () => HttpExecutor.SINGLETON.get[Seq[String], Error](s"http://$workerAddress/connectors"),
        "fetch active connectors")

      override def connectionProps: String = _connectionProps

      override def status(name: String)(implicit executionContext: ExecutionContext): Future[ConnectorInfo] = retry(
        () => HttpExecutor.SINGLETON.get[ConnectorInfo, Error](s"http://$workerAddress/connectors/$name/status"),
        s"status of $name")

      override def config(name: String)(implicit executionContext: ExecutionContext): Future[ConnectorConfig] = retry(
        () => HttpExecutor.SINGLETON.get[ConnectorConfig, Error](s"http://$workerAddress/connectors/$name/config"),
        s"config of $name")

      override def taskStatus(name: String, id: Int)(implicit executionContext: ExecutionContext): Future[TaskStatus] =
        retry(
          () =>
            HttpExecutor.SINGLETON.get[TaskStatus, Error](s"http://$workerAddress/connectors/$name/tasks/$id/status"),
          s"status of $name/$id")
      override def pause(name: String)(implicit executionContext: ExecutionContext): Future[Unit] =
        retry(() => HttpExecutor.SINGLETON.put[Error](s"http://$workerAddress/connectors/$name/pause"), s"pause $name")

      override def resume(name: String)(implicit executionContext: ExecutionContext): Future[Unit] = retry(
        () => HttpExecutor.SINGLETON.put[Error](s"http://$workerAddress/connectors/$name/resume"),
        s"resume $name")

      override def connectorValidator(): Validator =
        (executionContext, validation) => {
          implicit val exec: ExecutionContext = executionContext
          retry(
            () => {
              if (validation.topicNames().isEmpty)
                throw new IllegalArgumentException(
                  "I'm sorry for this error. However, please fill the topics" +
                    "for your validation request in order to test other settings. This prerequisite is introduced by kafka 2.x")
              HttpExecutor.SINGLETON
                .put[Validation, ConfigInfos, Error](
                  s"http://$workerAddress/connector-plugins/${validation.className()}/config/validate",
                  validation)
                .map(SettingInfo.of)
            },
            "connectorValidator"
          )
        }
    }
  }

  /**
    * a base class used to collect the setting from source/sink connector when creating
    */
  trait Creator {
    private[this] val connectorFormatter = ConnectorFormatter.of()

    /**
      * set the connector id. It should be a unique id.
      *
      * @param name connector name
      * @return this one
      */
    def name(name: String): Creator = {
      connectorFormatter.name(name)
      this
    }

    /**
      * set the connector class. The class must be loaded in class loader otherwise it will fail to create the connector.
      *
      * @param className connector class
      * @return this one
      */
    def className(className: String): Creator = {
      connectorFormatter.className(className)
      this
    }

    /**
      * set the connector class. The class must be loaded in class loader otherwise it will fail to create the connector.
      *
      * @param clz connector class
      * @return this one
      */
    def connectorClass[T](clz: Class[T]): Creator = className(Objects.requireNonNull(clz).getName)

    /**
      * set the topic in which you have interest.
      *
      * @param topicName topic
      * @return this one
      */
    def topicName(topicName: String): Creator = {
      connectorFormatter.topicName(topicName)
      this
    }

    /**
      * the max number from sink task you want to create
      *
      * @param numberOfTasks max number from sink task
      * @return this one
      */
    @Optional("default is 1")
    def numberOfTasks(numberOfTasks: Int): Creator = {
      connectorFormatter.numberOfTasks(numberOfTasks)
      this
    }

    /**
      * extra setting passed to sink connector. This setting is optional.
      *
      * @param settings setting
      * @return this one
      */
    @Optional("default is empty")
    def settings(settings: Map[String, String]): Creator = {
      connectorFormatter.settings(settings.asJava)
      this
    }

    /**
      * set the columns
      * @param columns columns
      * @return this builder
      */
    @Optional("default is all columns")
    def columns(columns: Seq[Column]): Creator = {
      connectorFormatter.columns(columns.asJava)
      this
    }

    /**
      * set the topics in which you have interest.
      *
      * @param topicNames topics
      * @return this one
      */
    def topicNames(topicNames: Seq[String]): Creator = {
      connectorFormatter.topicNames(topicNames.asJava)
      this
    }

    /**
      * setting the key converter. By default there is no converter in ohara connector since it enable us to retrieve/send
      * data to connector through topic. If you wrap the data by connector, your producer/consumer have to unwrap
      * data in order to access data correctly.
      *
      * @return this one
      */
    @Optional("default key converter is ConverterType.NONE")
    def converterTypeOfKey(converterTypeOfKey: ConverterType): Creator = {
      connectorFormatter.converterTypeOfKey(converterTypeOfKey)
      this
    }

    /**
      * setting the value converter. By default there is no converter in ohara connector since it enable us to retrieve/send
      * data to connector through topic. If you wrap the data by connector, your producer/consumer have to unwrap
      * data in order to access data correctly.
      *
      * @return this one
      */
    @Optional("default key converter is ConverterType.NONE")
    def converterTypeOfValue(converterTypeOfValue: ConverterType): Creator = {
      connectorFormatter.converterTypeOfValue(converterTypeOfValue)
      this
    }

    /**
      * send the request to create the sink connector.
      *
      * @return this one
      */
    def create()(implicit executionContext: ExecutionContext): Future[ConnectorCreationResponse] =
      doCreate(
        executionContext = Objects.requireNonNull(executionContext),
        creation = connectorFormatter.requestOfCreation()
      )

    /**
      * send the request to kafka worker
      *
      * @return response
      */
    protected def doCreate(executionContext: ExecutionContext, creation: Creation): Future[ConnectorCreationResponse]
  }

  trait Validator {
    private[this] val formatter: ConnectorFormatter = ConnectorFormatter.of()

    def className(className: String): Validator = {
      this.formatter.className(CommonUtils.requireNonEmpty(className))
      this
    }

    def connectorClass(clz: Class[_]): Validator = className(clz.getName)

    @Optional("Default is none")
    def setting(key: String, value: String): Validator = settings(
      Map(CommonUtils.requireNonEmpty(key) -> CommonUtils.requireNonEmpty(value)))

    @Optional("Default is none")
    def settings(settings: Map[String, String]): Validator = {
      this.formatter.settings(CommonUtils.requireNonEmpty(settings.asJava))
      this
    }

    /**
      * set the topic in which you have interest.
      *
      * @param topicName topic
      * @return this one
      */
    @Optional("Default is none")
    def topicName(topicName: String): Validator = topicNames(Seq(CommonUtils.requireNonEmpty(topicName)))

    /**
      * set the topic in which you have interest.
      *
      * @param topicNames topic
      * @return this one
      */
    @Optional("Default is none")
    def topicNames(topicNames: Seq[String]): Validator = {
      this.formatter.topicNames(CommonUtils.requireNonEmpty(topicNames.asJava))
      this
    }

    /**
      * the max number from sink task you want to create
      *
      * @param numberOfTasks max number from sink task
      * @return this one
      */
    @Optional("Default is none")
    def numberOfTasks(numberOfTasks: Int): Validator = {
      this.formatter.numberOfTasks(CommonUtils.requirePositiveInt(numberOfTasks))
      this
    }

    /**
      * set the columns
      * @param columns columns
      * @return this builder
      */
    @Optional("Default is none")
    def columns(columns: Seq[Column]): Validator = {
      this.formatter.columns(CommonUtils.requireNonEmpty(columns.asJava))
      this
    }

    def name(name: String): Validator = {
      this.formatter.name(name)
      this
    }

    def run()(implicit executionContext: ExecutionContext): Future[SettingInfo] = doValidate(
      executionContext = Objects.requireNonNull(executionContext),
      validation = formatter.requestOfValidation()
    )

    /**
      * send the request to kafka worker
      *
      * @return response
      */
    protected def doValidate(executionContext: ExecutionContext, validation: Validation): Future[SettingInfo]

  }
}
