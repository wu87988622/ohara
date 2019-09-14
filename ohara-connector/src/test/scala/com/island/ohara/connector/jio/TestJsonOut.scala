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

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.{HttpMethods, HttpRequest}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.island.ohara.client.kafka.WorkerClient
import com.island.ohara.common.data._
import com.island.ohara.common.setting.{ConnectorKey, TopicKey}
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.connector.ConnectorTestUtils
import com.island.ohara.kafka.Producer
import com.island.ohara.testing.WithBrokerWorker
import org.junit.{After, Test}
import org.scalatest.Matchers
import spray.json.DefaultJsonProtocol._
import spray.json.{JsNumber, JsString}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TestJsonOut extends WithBrokerWorker with Matchers {
  private[this] val workerClient = WorkerClient(testUtil.workersConnProps)

  private[this] val props = JioProps(CommonUtils.availablePort())

  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)

  private[this] def pushData(data: Seq[JioData], topicKey: TopicKey): Unit = pushRawData(data.map(_.row), topicKey)

  private[this] def pushRawData(data: Seq[Row], topicKey: TopicKey): Unit = {
    val producer = Producer
      .builder()
      .connectionProps(testUtil.brokersConnProps)
      .keySerializer(Serializer.ROW)
      .valueSerializer(Serializer.BYTES)
      .build()
    try {
      data.foreach(d => producer.sender().topicName(topicKey.topicNameOnKafka()).key(d).send().get())
    } finally producer.close()
  }

  private[this] def pollData(topicKey: TopicKey): Seq[JioData] = {
    implicit val actorSystem: ActorSystem = ActorSystem("Executor-TestJsonIn")
    implicit val actorMaterializer: ActorMaterializer = ActorMaterializer()
    try result(
      Http()
        .singleRequest(HttpRequest(HttpMethods.GET, s"http://localhost:${props.bindingPort}/${props.bindingPath}"))
        .flatMap(res => Unmarshal(res.entity).to[Seq[JioData]]))
    finally Releasable.close(() => Await.result(actorSystem.terminate(), props.closeTimeout))
  }

  private[this] def setupConnector(): TopicKey = setupConnector(props)

  private[this] def setupConnector(props: JioProps): TopicKey = {
    val topicKey = TopicKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    val connectorKey = ConnectorKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    result(
      workerClient
        .connectorCreator()
        .topicKey(topicKey)
        .connectorClass(classOf[JsonOut])
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
    pushData(data, topicKey)
    // connector is running in async mode so we have to wait data is pushed to connector
    CommonUtils.await(() => pollData(topicKey).size == 2, java.time.Duration.ofSeconds(60))
    val receivedData = pollData(topicKey)
    receivedData.size shouldBe data.size
    data.foreach { d =>
      // the order of sending data is random so we use the size to fetch correct data to compare
      d shouldBe receivedData.find(_.raw.size == d.raw.size).get
    }
  }

  @Test
  def testUnsupportedData(): Unit = {
    val topicKey = setupConnector()
    // array is not supported
    val unsupportedData = Row.of(Cell.of("abc", Row.of(Cell.of("a", "b"))))
    pushRawData(Seq(unsupportedData), topicKey)
    // the unsupported data is filter out so we can't fetch any data from JsonOut
    pollData(topicKey) shouldBe Seq.empty
    TimeUnit.SECONDS.sleep(5)
    // sleep to make sure there is no data :)
    pollData(topicKey) shouldBe Seq.empty
  }
  @Test
  def testBufferSize(): Unit = {
    val bufferSize = 3
    val dataSize = bufferSize * 5
    val topicKey = setupConnector(props.copy(bufferSize = bufferSize))
    val data = (0 until dataSize).map(
      _ =>
        JioData(
          Map(
            CommonUtils.randomString() -> JsString(CommonUtils.randomString())
          )))
    pushData(data, topicKey)
    // connector is running in async mode so we have to wait data is pushed to connector
    CommonUtils.await(() => pollData(topicKey).nonEmpty, java.time.Duration.ofSeconds(60))
    val receivedData = pollData(topicKey)
    // the size of data is larger than buffer size so some data must be discard
    receivedData.size shouldBe bufferSize
  }

  @After
  def tearDown(): Unit =
    result(Future.sequence(result(workerClient.activeConnectors()).map(workerClient.delete)))
}
