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

package com.island.ohara.configurator.route

import com.island.ohara.client.configurator.v0._
import com.island.ohara.common.rule.OharaTest
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.Configurator
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
class TestLogRoute extends OharaTest with Matchers {
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
    val cluster = result(zkApi.list()).head
    val clusterLogs = result(logApi.log4ZookeeperCluster(cluster.key))
    clusterLogs.clusterKey shouldBe cluster.key
    clusterLogs.logs.isEmpty shouldBe false
  }

  @Test
  def fetchLogFromBroker(): Unit = {
    val cluster = result(bkApi.list()).head
    val clusterLogs = result(logApi.log4BrokerCluster(cluster.key))
    clusterLogs.clusterKey shouldBe cluster.key
    clusterLogs.logs.isEmpty shouldBe false
  }

  @Test
  def fetchLogFromWorker(): Unit = {
    val cluster = result(wkApi.list()).head
    val clusterLogs = result(logApi.log4WorkerCluster(cluster.key))
    clusterLogs.clusterKey shouldBe cluster.key
    clusterLogs.logs.isEmpty shouldBe false
  }

  @Test
  def fetchLogFromStream(): Unit = {
    val file = result(fileApi.request.file(CommonUtils.createTempJar(CommonUtils.randomString(10))).upload())
    val fromTopic = result(topicApi.request.create())
    result(topicApi.start(fromTopic.key))
    val toTopic = result(topicApi.request.create())
    result(topicApi.start(toTopic.key))
    val nodeNames = result(zkApi.list()).head.nodeNames
    val cluster = result(
      streamApi.request
        .nodeNames(nodeNames)
        .fromTopicKey(fromTopic.key)
        .toTopicKey(toTopic.key)
        .jarKey(file.key)
        .create())
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

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
