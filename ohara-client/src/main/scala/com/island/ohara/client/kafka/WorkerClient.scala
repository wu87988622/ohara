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
import java.util.concurrent.TimeUnit

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.island.ohara.client.HttpExecutor
import com.island.ohara.client.kafka.WorkerJson._
import spray.json.DefaultJsonProtocol._

import scala.concurrent.{Await, Future}
import scala.util.Random
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * a helper class used to send the rest request to kafka worker.
  * TODO: DON'T block all methods ... by chia
  */
trait WorkerClient {

  def connectorCreator(): ConnectorCreator

  def delete(name: String): Unit

  def pause(name: String): Unit

  def resume(name: String): Unit

  def plugins(): Seq[Plugin]

  def activeConnectors(): Seq[String]

  def connectionProps: String

  def status(name: String): ConnectorInfo

  def config(name: String): ConnectorConfig

  def taskStatus(name: String, id: Int): TaskStatus

  /**
    * Check whether a connector name is used in creating connector (even if the connector fails to start, this method
    * still return true)
    * @param name connector name
    * @return true if connector exists
    */
  def exist(name: String): Boolean = activeConnectors().contains(name)

  def nonExist(name: String): Boolean = !exist(name)
}

object WorkerClient {
  def apply(_connectionProps: String): WorkerClient = {
    val workerList = _connectionProps.split(",")
    if (workerList.isEmpty) throw new IllegalArgumentException(s"Invalid workers:${_connectionProps}")
    new WorkerClient() with SprayJsonSupport {
      private[this] val workerAddress: String = workerList(Random.nextInt(workerList.size))

      import scala.concurrent.duration._
      private[this] def result[T](exec: () => Future[T]): T = Await.result(retry(exec), 30 seconds)

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

      override def connectorCreator(): ConnectorCreator = request =>
        result(
          () =>
            HttpExecutor.SINGLETON.post[CreateConnectorRequest, CreateConnectorResponse, Error](
              s"http://$workerAddress/connectors",
              request)
      )

      override def delete(name: String): Unit =
        result(() => HttpExecutor.SINGLETON.delete[Error](s"http://$workerAddress/connectors/$name"))

      override def plugins(): Seq[Plugin] = result(
        () => HttpExecutor.SINGLETON.get[Seq[Plugin], Error](s"http://$workerAddress/connector-plugins"))
      override def activeConnectors(): Seq[String] = result(
        () => HttpExecutor.SINGLETON.get[Seq[String], Error](s"http://$workerAddress/connectors"))

      override def connectionProps: String = _connectionProps

      override def status(name: String): ConnectorInfo = result(
        () => HttpExecutor.SINGLETON.get[ConnectorInfo, Error](s"http://$workerAddress/connectors/$name/status"))

      override def config(name: String): ConnectorConfig = result(
        () => HttpExecutor.SINGLETON.get[ConnectorConfig, Error](s"http://$workerAddress/connectors/$name/config"))

      override def taskStatus(name: String, id: Int): TaskStatus = result(
        () => HttpExecutor.SINGLETON.get[TaskStatus, Error](s"http://$workerAddress/connectors/$name/tasks/$id/status"))
      override def pause(name: String): Unit = result(
        () => HttpExecutor.SINGLETON.put[Error](s"http://$workerAddress/connectors/$name/pause"))

      override def resume(name: String): Unit = result(
        () => HttpExecutor.SINGLETON.put[Error](s"http://$workerAddress/connectors/$name/resume"))
    }
  }
}
