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

import com.island.ohara.client.configurator.v0.{BrokerApi, ConnectorApi, TopicApi, WorkerApi}
import com.island.ohara.common.data.{Column, DataType}
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.Configurator
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
class TestConnectorRoute extends SmallTest with Matchers {
  private[this] val configurator = Configurator.builder.fake(1, 1).build()

  private[this] val connectorApi = ConnectorApi.access.hostname(configurator.hostname).port(configurator.port)

  @Test
  def runConnectorWithoutTopic(): Unit = {
    val connector = result(
      connectorApi.request.name(CommonUtils.randomString(10)).className(CommonUtils.randomString(10)).create())

    an[IllegalArgumentException] should be thrownBy result(connectorApi.start(connector.name))
  }

  @Test
  def test(): Unit = {
    // test add
    result(connectorApi.list()).size shouldBe 0

    val columns = Seq(Column.builder().name("cf").dataType(DataType.BOOLEAN).order(1).build(),
                      Column.builder().name("cf").dataType(DataType.BOOLEAN).order(2).build())
    val name = CommonUtils.randomString()
    val className = CommonUtils.randomString()
    val numberOfTasks = 3
    val response = result(
      connectorApi.request.name(name).className(className).columns(columns).numberOfTasks(numberOfTasks).create())
    response.name shouldBe name
    response.className shouldBe className
    response.columns shouldBe columns
    response.numberOfTasks shouldBe numberOfTasks

    // test update
    val className2 = CommonUtils.randomString()
    val numberOfTasks2 = 5
    val columns2 = Seq(Column.builder().name("cf").dataType(DataType.BOOLEAN).order(1).build())
    val response2 = result(
      connectorApi.request
        .name(response.name)
        .className(className2)
        .columns(columns2)
        .numberOfTasks(numberOfTasks2)
        .update())
    response2.name shouldBe name
    response2.className shouldBe className2
    response2.columns shouldBe columns2
    response2.numberOfTasks shouldBe numberOfTasks2

    // test delete
    result(connectorApi.list()).size shouldBe 1
    result(connectorApi.delete(response.name))
    result(connectorApi.list()).size shouldBe 0

    // test nonexistent data
    an[IllegalArgumentException] should be thrownBy result(connectorApi.get(CommonUtils.randomString()))
  }

  @Test
  def testInvalidColumns(): Unit = {
    result(connectorApi.list()).size shouldBe 0

    val illegalOrder = Seq(Column.builder().name("cf").dataType(DataType.BOOLEAN).order(0).build(),
                           Column.builder().name("cf").dataType(DataType.BOOLEAN).order(2).build())

    an[IllegalArgumentException] should be thrownBy result(
      ConnectorApi.access
        .hostname(configurator.hostname)
        .port(configurator.port)
        .request
        .name(CommonUtils.randomString(10))
        .className(CommonUtils.randomString(10))
        .columns(illegalOrder)
        .create())

    result(connectorApi.list()).size shouldBe 0

    val duplicateOrder = Seq(Column.builder().name("cf").dataType(DataType.BOOLEAN).order(1).build(),
                             Column.builder().name("cf").dataType(DataType.BOOLEAN).order(1).build())

    an[IllegalArgumentException] should be thrownBy result(
      ConnectorApi.access
        .hostname(configurator.hostname)
        .port(configurator.port)
        .request
        .name(CommonUtils.randomString(10))
        .className(CommonUtils.randomString(10))
        .columns(duplicateOrder)
        .create())

    result(connectorApi.list()).size shouldBe 0
  }

  @Test
  def removeConnectorFromDeletedCluster(): Unit = {
    val connector = result(
      ConnectorApi.access
        .hostname(configurator.hostname)
        .port(configurator.port)
        .request
        .name(CommonUtils.randomString(10))
        .className(CommonUtils.randomString(10))
        .create())

    result(configurator.clusterCollie.workerCollie.remove(connector.workerClusterName))

    result(connectorApi.delete(connector.name))

    result(connectorApi.list()).exists(_.name == connector.name) shouldBe false
  }

  @Test
  def runConnectorOnNonexistentCluster(): Unit = {
    an[IllegalArgumentException] should be thrownBy result(
      ConnectorApi.access
        .hostname(configurator.hostname)
        .port(configurator.port)
        .request
        .name(CommonUtils.randomString(10))
        .className(CommonUtils.randomString(10))
        .workerClusterName(CommonUtils.randomString())
        .create())
  }

  @Test
  def runConnectorWithoutSpecificCluster(): Unit = {
    val bk = result(BrokerApi.access.hostname(configurator.hostname).port(configurator.port).list()).head

    val wk = result(
      WorkerApi.access
        .hostname(configurator.hostname)
        .port(configurator.port)
        .request
        .name(CommonUtils.randomString(10))
        .brokerClusterName(bk.name)
        .nodeNames(bk.nodeNames)
        .create())

    // there are two worker cluster so it fails to match worker cluster
    an[IllegalArgumentException] should be thrownBy result(
      ConnectorApi.access
        .hostname(configurator.hostname)
        .port(configurator.port)
        .request
        .name(CommonUtils.randomString(10))
        .className(CommonUtils.randomString(10))
        .create())

    //pass since we have assigned a worker cluster
    result(
      ConnectorApi.access
        .hostname(configurator.hostname)
        .port(configurator.port)
        .request
        .name(CommonUtils.randomString(10))
        .className(CommonUtils.randomString(10))
        .workerClusterName(wk.name)
        .create())

  }

  @Test
  def testIdempotentPause(): Unit = {
    val topic = result(
      TopicApi.access
        .hostname(configurator.hostname)
        .port(configurator.port)
        .request
        .name(CommonUtils.randomString(10))
        .create())

    val connector = result(
      ConnectorApi.access
        .hostname(configurator.hostname)
        .port(configurator.port)
        .request
        .name(CommonUtils.randomString(10))
        .className(CommonUtils.randomString(10))
        .topicName(topic.name)
        .create())

    result(connectorApi.start(connector.name)).state should not be None

    (0 to 10).foreach(_ => result(connectorApi.pause(connector.name)).state should not be None)
  }

  @Test
  def testIdempotentResume(): Unit = {
    val topic = result(
      TopicApi.access
        .hostname(configurator.hostname)
        .port(configurator.port)
        .request
        .name(CommonUtils.randomString(10))
        .create())

    val connector = result(
      ConnectorApi.access
        .hostname(configurator.hostname)
        .port(configurator.port)
        .request
        .name(CommonUtils.randomString(10))
        .className(CommonUtils.randomString(10))
        .topicName(topic.name)
        .create())

    result(connectorApi.start(connector.name)).state should not be None

    (0 to 10).foreach(_ => result(connectorApi.resume(connector.name)).state should not be None)
  }

  @Test
  def testIdempotentStop(): Unit = {
    val topic = result(
      TopicApi.access
        .hostname(configurator.hostname)
        .port(configurator.port)
        .request
        .name(CommonUtils.randomString(10))
        .create())

    val connector = result(
      ConnectorApi.access
        .hostname(configurator.hostname)
        .port(configurator.port)
        .request
        .name(CommonUtils.randomString(10))
        .className(CommonUtils.randomString(10))
        .topicName(topic.name)
        .create())

    result(connectorApi.start(connector.name)).state should not be None

    (0 to 10).foreach(_ => result(connectorApi.stop(connector.name)).state shouldBe None)
  }

  @Test
  def testIdempotentStart(): Unit = {
    val topic = result(
      TopicApi.access
        .hostname(configurator.hostname)
        .port(configurator.port)
        .request
        .name(CommonUtils.randomString(10))
        .create())

    val connector = result(
      ConnectorApi.access
        .hostname(configurator.hostname)
        .port(configurator.port)
        .request
        .name(CommonUtils.randomString(10))
        .className(CommonUtils.randomString(10))
        .topicName(topic.name)
        .create())

    result(connectorApi.start(connector.name)).state should not be None

    (0 to 10).foreach(_ => result(connectorApi.start(connector.name)).state should not be None)
  }

  @Test
  def failToChangeWorkerCluster(): Unit = {
    val originWkName = result(WorkerApi.access.hostname(configurator.hostname).port(configurator.port).list()).head.name

    val bk = result(BrokerApi.access.hostname(configurator.hostname).port(configurator.port).list()).head

    val wk = result(
      WorkerApi.access
        .hostname(configurator.hostname)
        .port(configurator.port)
        .request
        .name(CommonUtils.randomString(10))
        .brokerClusterName(bk.name)
        .nodeNames(bk.nodeNames)
        .create())
    val topic = result(
      TopicApi.access
        .hostname(configurator.hostname)
        .port(configurator.port)
        .request
        .name(CommonUtils.randomString(10))
        .create())

    val response = result(
      connectorApi.request
        .name(CommonUtils.randomString(10))
        .className(CommonUtils.randomString(10))
        .workerClusterName(originWkName)
        .topicName(topic.name)
        .create())

    an[IllegalArgumentException] should be thrownBy result(
      connectorApi.request
        .name(response.name)
        .className(CommonUtils.randomString(10))
        .workerClusterName(wk.name)
        .update())
  }

  @Test
  def defaultNumberOfTasksShouldExist(): Unit = {
    val connectorDesc = result(
      connectorApi.request.name(CommonUtils.randomString(10)).className(CommonUtils.randomString(10)).create())
    connectorDesc.numberOfTasks shouldBe ConnectorApi.DEFAULT_NUMBER_OF_TASKS

    result(connectorApi.request.name(CommonUtils.randomString(10)).className(CommonUtils.randomString(10)).update()).numberOfTasks shouldBe ConnectorApi.DEFAULT_NUMBER_OF_TASKS
  }

  @Test
  def testStartAnNonexistentConnector(): Unit = {
    an[IllegalArgumentException] should be thrownBy result(connectorApi.start(methodName()))
  }

  @Test
  def testStopAnNonexistentConnector(): Unit = {
    an[IllegalArgumentException] should be thrownBy result(connectorApi.stop(methodName()))
  }
  @Test
  def testPauseAnNonexistentConnector(): Unit = {
    an[IllegalArgumentException] should be thrownBy result(connectorApi.pause(methodName()))
  }

  @Test
  def testResumeAnNonexistentConnector(): Unit = {
    an[IllegalArgumentException] should be thrownBy result(connectorApi.resume(methodName()))
  }

  @Test
  def testParseColumnJson(): Unit = {
    import spray.json._
    val request = ConnectorApi.COLUMN_JSON_FORMAT.read("""
                                                         |{
                                                         |  "name":"cf",
                                                         |  "dataType":"boolean",
                                                         |  "order":1
                                                         |}
                                                       """.stripMargin.parseJson)
    request.name shouldBe "cf"
    request.newName shouldBe "cf"
    request.dataType shouldBe DataType.BOOLEAN
    request.order shouldBe 1

    val request2 = ConnectorApi.COLUMN_JSON_FORMAT.read("""
                                                          |{
                                                          |  "name":"cf",
                                                          |  "newName":null,
                                                          |  "dataType":"boolean",
                                                          |  "order":1
                                                          |}
                                                        """.stripMargin.parseJson)
    request2 shouldBe request

    val request3 = ConnectorApi.COLUMN_JSON_FORMAT.read("""
                                                          |{
                                                          |  "name":"cf",
                                                          |  "newName":"cf",
                                                          |  "dataType":"boolean",
                                                          |  "order":1
                                                          |}
                                                        """.stripMargin.parseJson)
    request3 shouldBe request
  }

  @Test
  def testParseCreationJson(): Unit = {
    import spray.json._
    val request =
      ConnectorApi.CONNECTOR_CREATION_JSON_FORMAT.read(
        """
          |{
          |  "name":"perf",
          |  "connector.class":"com.island.ohara.connector.perf.PerfSource",
          |  "topics":["59e9010c-fd9c-4a41-918a-dacc9b84aa2b"],
          |  "tasks.max":1,
          |  "perf.batch":"1",
          |  "perf.frequence":"2 seconds",
          |  "columns":[{
          |    "name": "cf0",
          |    "newName": "cf0",
          |    "dataType": "int",
          |    "order": 1
          |  },{
          |    "name": "cf1",
          |    "newName": "cf1",
          |    "dataType": "bytes",
          |    "order": 2
          |  }]
          |}
        """.stripMargin.parseJson)
    request.className shouldBe "com.island.ohara.connector.perf.PerfSource"
    request.topicNames.head shouldBe "59e9010c-fd9c-4a41-918a-dacc9b84aa2b"
    request.numberOfTasks shouldBe 1
    request.plain("perf.batch") shouldBe "1"
    request.columns.size shouldBe 2
    request.columns.head shouldBe Column.builder().name("cf0").newName("cf0").dataType(DataType.INT).order(1).build()
    request.columns.last shouldBe Column.builder().name("cf1").newName("cf1").dataType(DataType.BYTES).order(2).build()
    request.tags shouldBe Set.empty
  }

  @Test
  def updateTags(): Unit = {
    val tags = Set(CommonUtils.randomString(10), CommonUtils.randomString(10))
    val connectorDesc = result(connectorApi.request.tags(tags).create())
    connectorDesc.tags shouldBe tags

    val tags2 = Set(CommonUtils.randomString(10), CommonUtils.randomString(10))
    val connectorDesc2 = result(connectorApi.request.name(connectorDesc.name).tags(tags2).update())
    connectorDesc2.tags shouldBe tags2

    val connectorDesc3 = result(connectorApi.request.name(connectorDesc.name).update())
    connectorDesc3.tags shouldBe tags2

    val connectorDesc4 = result(connectorApi.request.name(connectorDesc.name).tags(Set.empty).update())
    connectorDesc4.tags shouldBe Set.empty
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
