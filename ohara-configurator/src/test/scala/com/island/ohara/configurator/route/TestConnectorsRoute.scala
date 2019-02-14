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

import com.island.ohara.client.configurator.v0.ConnectorApi
import com.island.ohara.client.configurator.v0.ConnectorApi.{ConnectorCreationRequest, ConnectorInfo}
import com.island.ohara.common.data.{Column, DataType}
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.Releasable
import com.island.ohara.configurator.Configurator
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TestConnectorsRoute extends SmallTest with Matchers {
  private[this] val configurator = Configurator.builder().fake(1, 1).build()

  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)

  private[this] val connectorApi = ConnectorApi.access().hostname(configurator.hostname).port(configurator.port)

  @Test
  def runConnectorWithoutTopic(): Unit = {
    val connector = result(
      connectorApi.add(
        ConnectorCreationRequest(name = methodName,
                                 className = "jdbc",
                                 schema = Seq.empty,
                                 configs = Map("c0" -> "v0", "c1" -> "v1"),
                                 topics = Seq.empty,
                                 numberOfTasks = 1)))

    an[IllegalArgumentException] should be thrownBy result(connectorApi.start(connector.id))
  }

  @Test
  def testSource(): Unit = {
    def compareRequestAndResponse(request: ConnectorCreationRequest, response: ConnectorInfo): ConnectorInfo = {
      request.name shouldBe response.name
      request.schema shouldBe response.schema
      request.configs shouldBe response.configs
      response
    }

    def compare2Response(lhs: ConnectorInfo, rhs: ConnectorInfo): Unit = {
      lhs.id shouldBe rhs.id
      lhs.name shouldBe rhs.name
      lhs.schema shouldBe rhs.schema
      lhs.configs shouldBe rhs.configs
      lhs.lastModified shouldBe rhs.lastModified
    }

    val schema = Seq(Column.of("cf", DataType.BOOLEAN, 1), Column.of("cf", DataType.BOOLEAN, 2))
    // test add
    result(connectorApi.list()).size shouldBe 0
    val request = ConnectorCreationRequest(name = methodName,
                                           className = "jdbc",
                                           schema = schema,
                                           configs = Map("c0" -> "v0", "c1" -> "v1"),
                                           topics = Seq.empty,
                                           numberOfTasks = 1)
    val response =
      compareRequestAndResponse(request, result(connectorApi.add(request)))

    // test get
    compare2Response(response, result(connectorApi.get(response.id)))

    // test update
    val anotherRequest = ConnectorCreationRequest(name = methodName,
                                                  className = "jdbc",
                                                  schema = schema,
                                                  configs = Map("c0" -> "v0", "c1" -> "v1", "c2" -> "v2"),
                                                  topics = Seq.empty,
                                                  numberOfTasks = 1)
    val newResponse =
      compareRequestAndResponse(anotherRequest, result(connectorApi.update(response.id, anotherRequest)))

    // test get
    compare2Response(newResponse, result(connectorApi.get(newResponse.id)))

    // test delete
    result(connectorApi.list()).size shouldBe 1
    result(connectorApi.delete(response.id)) shouldBe newResponse
    result(connectorApi.list()).size shouldBe 0

    // test nonexistent data
    an[IllegalArgumentException] should be thrownBy result(connectorApi.get("asdasdasd"))
    an[IllegalArgumentException] should be thrownBy result(connectorApi.update("Asdasd", anotherRequest))
  }

  @Test
  def testInvalidSource(): Unit = {
    result(connectorApi.list()).size shouldBe 0

    val illegalOrder = Seq(Column.of("cf", DataType.BOOLEAN, 0), Column.of("cf", DataType.BOOLEAN, 2))
    an[IllegalArgumentException] should be thrownBy result(
      connectorApi.add(
        ConnectorCreationRequest(name = methodName,
                                 className = "jdbc",
                                 schema = illegalOrder,
                                 configs = Map("c0" -> "v0", "c1" -> "v1"),
                                 topics = Seq.empty,
                                 numberOfTasks = 1)))
    result(connectorApi.list()).size shouldBe 0

    val duplicateOrder = Seq(Column.of("cf", DataType.BOOLEAN, 1), Column.of("cf", DataType.BOOLEAN, 1))
    an[IllegalArgumentException] should be thrownBy result(
      connectorApi.add(
        ConnectorCreationRequest(name = methodName,
                                 className = "jdbc",
                                 schema = duplicateOrder,
                                 configs = Map("c0" -> "v0", "c1" -> "v1"),
                                 topics = Seq.empty,
                                 numberOfTasks = 1)))
    result(connectorApi.list()).size shouldBe 0
  }

  @Test
  def testSink(): Unit = {
    def compareRequestAndResponse(request: ConnectorCreationRequest, response: ConnectorInfo): ConnectorInfo = {
      request.name shouldBe response.name
      request.configs shouldBe response.configs
      response
    }

    def compare2Response(lhs: ConnectorInfo, rhs: ConnectorInfo): Unit = {
      lhs.id shouldBe rhs.id
      lhs.name shouldBe rhs.name
      lhs.schema shouldBe rhs.schema
      lhs.configs shouldBe rhs.configs
      lhs.lastModified shouldBe rhs.lastModified
    }

    val schema = Seq(Column.of("cf", DataType.BOOLEAN, 1), Column.of("cf", DataType.BOOLEAN, 2))

    // test add
    result(connectorApi.list()).size shouldBe 0
    val request = ConnectorCreationRequest(name = methodName,
                                           className = "jdbc",
                                           schema = schema,
                                           configs = Map("c0" -> "v0", "c1" -> "v1"),
                                           topics = Seq.empty,
                                           numberOfTasks = 1)
    val response =
      compareRequestAndResponse(request, result(connectorApi.add(request)))

    // test get
    compare2Response(response, result(connectorApi.get(response.id)))

    // test update
    val anotherRequest = ConnectorCreationRequest(name = methodName,
                                                  className = "jdbc",
                                                  schema = schema,
                                                  configs = Map("c0" -> "v0", "c1" -> "v1", "c2" -> "v2"),
                                                  topics = Seq.empty,
                                                  numberOfTasks = 1)
    val newResponse =
      compareRequestAndResponse(anotherRequest, result(connectorApi.update(response.id, anotherRequest)))

    // test get
    compare2Response(newResponse, result(connectorApi.get(newResponse.id)))

    // test delete
    result(connectorApi.list()).size shouldBe 1
    result(connectorApi.delete(response.id)) shouldBe newResponse
    result(connectorApi.list()).size shouldBe 0

    // test nonexistent data
    an[IllegalArgumentException] should be thrownBy result(connectorApi.get("asdasdasd"))
    an[IllegalArgumentException] should be thrownBy result(connectorApi.update("Asdasd", anotherRequest))
  }

  @Test
  def testInvalidSink(): Unit = {

    result(connectorApi.list()).size shouldBe 0

    val illegalOrder = Seq(Column.of("cf", DataType.BOOLEAN, 0), Column.of("cf", DataType.BOOLEAN, 2))
    an[IllegalArgumentException] should be thrownBy result(
      connectorApi.add(
        ConnectorCreationRequest(name = methodName,
                                 className = "jdbc",
                                 schema = illegalOrder,
                                 configs = Map("c0" -> "v0", "c1" -> "v1"),
                                 topics = Seq.empty,
                                 numberOfTasks = 1)))
    result(connectorApi.list()).size shouldBe 0

    val duplicateOrder = Seq(Column.of("cf", DataType.BOOLEAN, 1), Column.of("cf", DataType.BOOLEAN, 1))
    an[IllegalArgumentException] should be thrownBy result(
      connectorApi.add(
        ConnectorCreationRequest(name = methodName,
                                 className = "jdbc",
                                 schema = duplicateOrder,
                                 configs = Map("c0" -> "v0", "c1" -> "v1"),
                                 topics = Seq.empty,
                                 numberOfTasks = 1)))
    result(connectorApi.list()).size shouldBe 0
  }

  @Test
  def removeConnectorFromDeletedCluster(): Unit = {
    val connector = result(
      connectorApi.add(
        ConnectorCreationRequest(name = methodName,
                                 className = "jdbc",
                                 schema = Seq.empty,
                                 configs = Map("c0" -> "v0", "c1" -> "v1", "c2" -> "v2"),
                                 topics = Seq.empty,
                                 numberOfTasks = 1)))

    val wk = result(configurator.clusterCollie.workerCollie().remove(connector.workerClusterName))
    wk.name shouldBe connector.workerClusterName

    result(connectorApi.delete(connector.id))

    result(connectorApi.list()).exists(_.id == connector.id) shouldBe false
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
