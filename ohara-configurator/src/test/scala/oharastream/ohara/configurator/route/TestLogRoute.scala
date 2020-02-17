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

package oharastream.ohara.configurator.route

import java.text.SimpleDateFormat

import oharastream.ohara.client.configurator.v0._
import oharastream.ohara.common.rule.OharaTest
import oharastream.ohara.common.setting.ObjectKey
import oharastream.ohara.common.util.{CommonUtils, Releasable}
import oharastream.ohara.configurator.Configurator
import org.junit.{After, Test}
import org.scalatest.Matchers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
class TestLogRoute extends OharaTest {
  private[this] val configurator = Configurator.builder.fake().build()

  private[this] val logApi = LogApi.access.hostname(configurator.hostname).port(configurator.port)

  private[this] val zkApi = ZookeeperApi.access.hostname(configurator.hostname).port(configurator.port)

  private[this] val bkApi = BrokerApi.access.hostname(configurator.hostname).port(configurator.port)

  private[this] val wkApi = WorkerApi.access.hostname(configurator.hostname).port(configurator.port)

  private[this] val streamApi = StreamApi.access.hostname(configurator.hostname).port(configurator.port)

  private[this] val fileApi = FileInfoApi.access.hostname(configurator.hostname).port(configurator.port)

  private[this] val topicApi = TopicApi.access.hostname(configurator.hostname).port(configurator.port)

  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)

  @Test
  def fetchLogFromZookeeper(): Unit = {
    val cluster     = result(zkApi.list()).head
    val clusterLogs = result(logApi.log4ZookeeperCluster(cluster.key))
    clusterLogs.clusterKey shouldBe cluster.key
    clusterLogs.logs.isEmpty shouldBe false
  }

  @Test
  def fetchLogFromBroker(): Unit = {
    val cluster     = result(bkApi.list()).head
    val clusterLogs = result(logApi.log4BrokerCluster(cluster.key))
    clusterLogs.clusterKey shouldBe cluster.key
    clusterLogs.logs.isEmpty shouldBe false
  }

  @Test
  def fetchLogFromWorker(): Unit = {
    val cluster     = result(wkApi.list()).head
    val clusterLogs = result(logApi.log4WorkerCluster(cluster.key))
    clusterLogs.clusterKey shouldBe cluster.key
    clusterLogs.logs.isEmpty shouldBe false
  }

  @Test
  def fetchLogFromStream(): Unit = {
    val file      = result(fileApi.request.file(RouteUtils.streamFile).upload())
    val fromTopic = result(topicApi.request.brokerClusterKey(result(bkApi.list()).head.key).create())
    result(topicApi.start(fromTopic.key))
    result(topicApi.get(fromTopic.key)).state should not be None
    val toTopic = result(topicApi.request.brokerClusterKey(result(bkApi.list()).head.key).create())
    result(topicApi.start(toTopic.key))
    result(topicApi.get(toTopic.key)).state should not be None
    val nodeNames = result(zkApi.list()).head.nodeNames
    val cluster = result(
      streamApi.request
        .nodeNames(nodeNames)
        .fromTopicKey(fromTopic.key)
        .toTopicKey(toTopic.key)
        .jarKey(file.key)
        .brokerClusterKey(
          result(BrokerApi.access.hostname(configurator.hostname).port(configurator.port).list()).head.key
        )
        .create()
    )
    result(streamApi.start(cluster.key))
    val clusterLogs = result(logApi.log4StreamCluster(cluster.key))
    clusterLogs.clusterKey shouldBe cluster.key
    clusterLogs.logs.isEmpty shouldBe false
  }

  @Test
  def fetchLogFromUnknown(): Unit = {
    val unknownKey = ObjectKey.of("default", CommonUtils.randomString(10))
    an[IllegalArgumentException] should be thrownBy result(logApi.log4ZookeeperCluster(unknownKey))
    an[IllegalArgumentException] should be thrownBy result(logApi.log4BrokerCluster(unknownKey))
    an[IllegalArgumentException] should be thrownBy result(logApi.log4WorkerCluster(unknownKey))
  }

  /**
    * in unit test, the configurator is NOT on docker container. Hence, we can't get log...
    */
  @Test
  def fetchLogFromConfigurator(): Unit = result(logApi.log4Configurator())

  @Test
  def testSinceSeconds(): Unit = {
    val df           = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS")
    val minTime      = df.parse("2019-12-17 03:20:00,339").getTime
    val weirdString0 = CommonUtils.randomString()
    val weirdString1 = CommonUtils.randomString()
    val strings: Iterator[String] = new Iterator[String] {
      private[this] val lines = Seq(
        "2019-12-17 03:20:00,337 INFO  [main] configurator.Configurator$(391): start a configurator built on hostname:node00 and port:12345",
        weirdString0,
        "2019-12-17 03:20:00,339 INFO  [main] configurator.Configurator$(393): enter ctrl+c to terminate the configurator",
        weirdString1,
        "2019-12-17 03:20:38,352 INFO  [main] configurator.Configurator$(397): Current data size:0"
      )
      private[this] var index       = 0
      override def hasNext: Boolean = index < lines.size
      override def next(): String =
        try lines(index)
        finally index += 1
    }
    val result = LogRoute.seekLogByTimestamp(strings, minTime)
    result should not include ("configurator.Configurator$(391)")
    result should not include (weirdString0)
    result should include(weirdString1)
    result should include("configurator.Configurator$(397)")
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
