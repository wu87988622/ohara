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

package com.island.ohara.connector.jio

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, RequestEntity, StatusCode}
import com.island.ohara.client.kafka.WorkerClient
import com.island.ohara.common.data._
import com.island.ohara.common.setting.{ConnectorKey, TopicKey}
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.connector.ConnectorTestUtils
import com.island.ohara.kafka.Consumer
import com.island.ohara.testing.WithBrokerWorker
import org.junit.{After, Test}
import org.scalatest.Matchers
import spray.json.{JsArray, JsNumber, JsObject, JsString, JsValue}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TestJsonIn extends WithBrokerWorker with Matchers {
  private[this] val workerClient = WorkerClient(testUtil.workersConnProps)

  private[this] val props = JioProps(CommonUtils.availablePort())

  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)

  private[this] def pushData(data: Seq[JioData]): Seq[StatusCode] = pushRawData(data.map(JioData.JIO_DATA_FORMAT.write))

  private[this] def pushRawData(data: Seq[JsValue]): Seq[StatusCode] = {
    implicit val actorSystem: ActorSystem = ActorSystem("Executor-TestJsonIn")
    def post(request: JsValue) = {
      Marshal(request).to[RequestEntity].flatMap { entity =>
        Http().singleRequest(
          HttpRequest(HttpMethods.POST, s"http://localhost:${props.bindingPort}/${props.bindingPath}", entity = entity))
      }
    }

    try result(Future.sequence(data.map(post))).map(_.status)
    finally Releasable.close(() => Await.result(actorSystem.terminate(), props.closeTimeout))
  }

  private[this] def pollData(topicKey: TopicKey, timeout: Duration, size: Int): Seq[JioData] = {
    val consumer = Consumer
      .builder()
      .topicName(topicKey.topicNameOnKafka)
      .offsetFromBegin()
      .connectionProps(testUtil.brokersConnProps)
      .keySerializer(Serializer.ROW)
      .valueSerializer(Serializer.BYTES)
      .build()
    try consumer
      .poll(java.time.Duration.ofNanos(timeout.toNanos), size)
      .asScala
      .filter(_.key().isPresent)
      .map(_.key().get())
      .map(JioData(_))
    finally consumer.close()
  }

  private[this] def setupConnector(): TopicKey = {
    val topicKey = TopicKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    val connectorKey = ConnectorKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    result(
      workerClient
        .connectorCreator()
        .topicKey(topicKey)
        .connectorClass(classOf[JsonIn])
        .numberOfTasks(1)
        .connectorKey(connectorKey)
        .settings(props.plain)
        .create())
    ConnectorTestUtils.checkConnector(testUtil, connectorKey)
    topicKey
  }

  @Test
  def testNormalCase(): Unit = {
    val topicKey = setupConnector()
    val data = Seq(
      JioData(
        Map(
          "a" -> JsString(CommonUtils.randomString())
        )),
      JioData(
        Map(
          "c" -> JsString(CommonUtils.randomString()),
          "d" -> JsNumber(100)
        ))
    )
    pushData(data).foreach(_.isSuccess() shouldBe true)
    val receivedData = pollData(topicKey, 30 seconds, data.size)
    receivedData.size shouldBe data.size
    data.foreach { d =>
      // the order of sending data is random so we use the size to fetch correct data to compare
      d shouldBe receivedData.find(_.raw.size == d.raw.size).get
    }
  }

  @Test
  def testUnsupportedData(): Unit = {
    setupConnector()
    // array is not supported
    val unsupportedData = JsObject("a" -> JsArray(Vector(JsString(CommonUtils.randomString()))))
    pushRawData(Seq(unsupportedData)).foreach(_.isSuccess() shouldBe false)
  }

  @After
  def tearDown(): Unit =
    result(Future.sequence(result(workerClient.activeConnectors()).map(workerClient.delete)))
}
