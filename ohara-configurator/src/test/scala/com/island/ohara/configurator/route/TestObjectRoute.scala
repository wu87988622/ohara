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

import com.island.ohara.client.configurator.v0.{BrokerApi, ConnectorApi, ObjectApi, TopicApi, WorkerApi}
import com.island.ohara.common.rule.OharaTest
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.Configurator
import org.junit.{After, Test}
import org.scalatest.Matchers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
class TestObjectRoute extends OharaTest {
  private[this] val configurator = Configurator.builder.fake(1, 1).build()

  private[this] val objectApi = ObjectApi.access.hostname(configurator.hostname).port(configurator.port)

  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)

  private[this] val initialSize = result(objectApi.list()).size

  private[this] val connectorApi = ConnectorApi.access.hostname(configurator.hostname).port(configurator.port)

  private[this] val workerClusterInfo = result(
    WorkerApi.access.hostname(configurator.hostname).port(configurator.port).list()
  ).head

  @Test
  def testTopic(): Unit = {
    val topic0 = result(
      TopicApi.access
        .hostname(configurator.hostname)
        .port(configurator.port)
        .request
        .name(CommonUtils.randomString(10))
        .brokerClusterKey(
          result(BrokerApi.access.hostname(configurator.hostname).port(configurator.port).list()).head.key
        )
        .create()
    )

    var objs = result(objectApi.list())
    objs.size shouldBe (1 + initialSize)

    objs.find(_.name == topic0.name).get.name shouldBe topic0.name
    objs.find(_.name == topic0.name).get.kind shouldBe topic0.kind
    objs.find(_.name == topic0.name).get.lastModified shouldBe topic0.lastModified

    val topic1 = result(
      TopicApi.access
        .hostname(configurator.hostname)
        .port(configurator.port)
        .request
        .name(CommonUtils.randomString(10))
        .brokerClusterKey(
          result(BrokerApi.access.hostname(configurator.hostname).port(configurator.port).list()).head.key
        )
        .create()
    )

    objs = result(objectApi.list())

    objs.size shouldBe (2 + initialSize)

    objs.find(_.name == topic1.name).get.name shouldBe topic1.name
    objs.find(_.name == topic1.name).get.kind shouldBe topic1.kind
    objs.find(_.name == topic1.name).get.lastModified shouldBe topic1.lastModified
  }

  @Test
  def listTopicAndConnector(): Unit = {
    val topic = result(
      TopicApi.access
        .hostname(configurator.hostname)
        .port(configurator.port)
        .request
        .name(CommonUtils.randomString(10))
        .brokerClusterKey(
          result(BrokerApi.access.hostname(configurator.hostname).port(configurator.port).list()).head.key
        )
        .create()
    )

    val connector = result(
      connectorApi.request
        .name(CommonUtils.randomString(10))
        .className("com.island.ohara.connector.ftp.FtpSink")
        .numberOfTasks(1)
        .workerClusterKey(workerClusterInfo.key)
        .topicKey(topic.key)
        .create()
    )

    val objs = result(objectApi.list())

    objs.size shouldBe (2 + initialSize)

    objs.find(_.name == connector.name).get.name shouldBe connector.name
    objs.find(_.name == connector.name).get.kind shouldBe connector.kind
    objs.find(_.name == connector.name).get.lastModified shouldBe connector.lastModified

    objs.find(_.name == topic.name).get.name shouldBe topic.name
    objs.find(_.name == topic.name).get.kind shouldBe topic.kind
    objs.find(_.name == topic.name).get.lastModified shouldBe topic.lastModified
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
