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

import com.island.ohara.client.configurator.v0.ConnectorApi.ConnectorCreationRequest
import com.island.ohara.client.configurator.v0.TopicApi._
import com.island.ohara.client.configurator.v0.{ConnectorApi, ObjectApi, TopicApi}
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.Configurator
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TestObjectRoute extends SmallTest with Matchers {

  private[this] val configurator = Configurator.builder().fake(1, 1).build()

  private[this] val objectApi = ObjectApi.access().hostname(configurator.hostname).port(configurator.port)

  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)

  private[this] val initialSize = result(objectApi.list()).size

  @Test
  def testTopic(): Unit = {
    val topic0 = result(
      TopicApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(
          TopicCreationRequest(name = Some(CommonUtils.randomString(10)),
                               brokerClusterName = None,
                               numberOfPartitions = None,
                               numberOfReplications = None)))

    var objs = result(objectApi.list())
    objs.size shouldBe (1 + initialSize)

    objs.find(_.id == topic0.id).get.name shouldBe topic0.name
    objs.find(_.id == topic0.id).get.kind shouldBe topic0.kind
    objs.find(_.id == topic0.id).get.lastModified shouldBe topic0.lastModified

    val topic1 = result(
      TopicApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(
          TopicCreationRequest(name = Some(CommonUtils.randomString(10)),
                               brokerClusterName = None,
                               numberOfPartitions = None,
                               numberOfReplications = None)))

    objs = result(objectApi.list())

    objs.size shouldBe (2 + initialSize)

    objs.find(_.id == topic1.id).get.name shouldBe topic1.name
    objs.find(_.id == topic1.id).get.kind shouldBe topic1.kind
    objs.find(_.id == topic1.id).get.lastModified shouldBe topic1.lastModified
  }

  @Test
  def testConnector(): Unit = {
    val connector0 = result(
      ConnectorApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(ConnectorCreationRequest(
          name = Some(CommonUtils.randomString(10)),
          workerClusterName = None,
          className = "com.island.ohara.connector.ftp.FtpSink",
          schema = Seq.empty,
          topics = Seq.empty,
          numberOfTasks = 1,
          configs = Map.empty
        )))

    var objs = result(objectApi.list())
    objs.size shouldBe (1 + initialSize)

    objs.find(_.id == connector0.id).get.name shouldBe connector0.name
    objs.find(_.id == connector0.id).get.kind shouldBe connector0.kind
    objs.find(_.id == connector0.id).get.lastModified shouldBe connector0.lastModified

    val connector1 = result(
      ConnectorApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(ConnectorCreationRequest(
          name = Some(CommonUtils.randomString(10)),
          workerClusterName = None,
          className = "com.island.ohara.connector.ftp.FtpSink",
          schema = Seq.empty,
          topics = Seq.empty,
          numberOfTasks = 1,
          configs = Map.empty
        )))

    objs = result(objectApi.list())

    objs.size shouldBe (2 + initialSize)

    objs.find(_.id == connector1.id).get.name shouldBe connector1.name
    objs.find(_.id == connector1.id).get.kind shouldBe connector1.kind
    objs.find(_.id == connector1.id).get.lastModified shouldBe connector1.lastModified
  }

  @Test
  def listTopicAndConnector(): Unit = {
    val connector = result(
      ConnectorApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(ConnectorCreationRequest(
          name = Some(CommonUtils.randomString(10)),
          workerClusterName = None,
          className = "com.island.ohara.connector.ftp.FtpSink",
          schema = Seq.empty,
          topics = Seq.empty,
          numberOfTasks = 1,
          configs = Map.empty
        )))

    val topic = result(
      TopicApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(
          TopicCreationRequest(name = Some(CommonUtils.randomString(10)),
                               brokerClusterName = None,
                               numberOfPartitions = None,
                               numberOfReplications = None)))

    val objs = result(objectApi.list())

    objs.size shouldBe (2 + initialSize)

    objs.find(_.id == connector.id).get.name shouldBe connector.name
    objs.find(_.id == connector.id).get.kind shouldBe connector.kind
    objs.find(_.id == connector.id).get.lastModified shouldBe connector.lastModified

    objs.find(_.id == topic.id).get.name shouldBe topic.name
    objs.find(_.id == topic.id).get.kind shouldBe topic.kind
    objs.find(_.id == topic.id).get.lastModified shouldBe topic.lastModified
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
