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

package com.island.ohara.client.configurator.v0

import com.island.ohara.client.configurator.v0.ConnectorApi.{ConnectorCreationRequest, _}
import com.island.ohara.common.data.{Column, DataType, Serializer}
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.kafka.connector.json.ConnectorFormatter
import org.junit.Test
import org.scalatest.Matchers
import spray.json.{JsArray, JsString, _}
class TestConnectorApi extends SmallTest with Matchers {

  @Test
  def testPlain(): Unit = {
    val name = CommonUtils.randomString()
    val className = CommonUtils.randomString()
    val workerClusterName = CommonUtils.randomString()
    val topicName = CommonUtils.randomString()
    val numberOfTasks = 10
    val (key, value) = ("aaa", "ccc")
    val column = Column.newBuilder().name("aa").newName("cc").dataType(DataType.FLOAT).order(10).build()
    val request = ConnectorCreationRequest(
      name = Some(name),
      className = Some(className),
      columns = Seq(column),
      topicNames = Seq(topicName),
      numberOfTasks = Some(numberOfTasks),
      settings = Map(key -> value),
      workerClusterName = Some(workerClusterName)
    )
    request.name.get shouldBe name
    request.className shouldBe className
    request.workerClusterName.get shouldBe workerClusterName
    request.topicNames.size shouldBe 1
    request.topicNames.head shouldBe topicName
    request.numberOfTasks shouldBe numberOfTasks
    request.columns.size shouldBe 1
    request.columns.head shouldBe column
    request.plain(key) shouldBe value
  }

  // TODO: remove this test after ohara manager starts to use new APIs
  @Test
  def testStaleCreationApis(): Unit = {
    val name = CommonUtils.randomString()
    val workerClusterName = CommonUtils.randomString()
    val className = CommonUtils.randomString()
    val column = Column
      .newBuilder()
      .name(CommonUtils.randomString())
      .newName(CommonUtils.randomString())
      .dataType(DataType.DOUBLE)
      .build()
    val topicNames = Seq(CommonUtils.randomString())
    val numberOfTasks = 10
    val configs = Map("aaa" -> "cccc")
    val anotherKey = CommonUtils.randomString()
    val anotherValue = CommonUtils.randomString()
    val request =
      CONNECTOR_CREATION_REQUEST_JSON_FORMAT.read(s"""
                                               |{
                                               |  "name": ${JsString(name).toString()},
                                               |  "workerClusterName": ${JsString(workerClusterName).toString()},
                                               |  "className": ${JsString(className).toString()},
                                               |  "schema": ${JsArray(COLUMN_JSON_FORMAT.write(column)).toString()},
                                               |  "topics": ${JsArray(topicNames.map(v => JsString(v)).toVector)
                                                       .toString()},
                                               |  "numberOfTasks": ${JsNumber(numberOfTasks).toString()},
                                               |  "settings": ${JsObject(
                                                       configs.map { case (k, v) => k -> JsString(v) }).toString()},
                                                  "$anotherKey": "$anotherValue"
                                               |}
                                            """.stripMargin.parseJson)
    request.name.get shouldBe name
    request.workerClusterName.get shouldBe workerClusterName
    request.className shouldBe className
    request.columns.head shouldBe column
    request.topicNames shouldBe topicNames
    request.numberOfTasks shouldBe numberOfTasks
    request.settings("aaa").asInstanceOf[JsString].value shouldBe "cccc"
    request.settings(anotherKey).asInstanceOf[JsString].value shouldBe anotherValue
    CONNECTOR_CREATION_REQUEST_JSON_FORMAT.read(CONNECTOR_CREATION_REQUEST_JSON_FORMAT.write(request)) shouldBe request
  }

  @Test
  def testSerialization(): Unit = {
    val request = ConnectorCreationRequest(
      settings = Map(
        "abc" -> JsString("Asdasdasd"),
        "ccc" -> JsNumber(312313),
        "bbb" -> JsArray(JsString("Asdasdasd"), JsString("aaa")),
        "ddd" -> JsObject("asdasd" -> JsString("Asdasdasd"))
      ))
    request shouldBe Serializer.OBJECT.from(Serializer.OBJECT.to(request)).asInstanceOf[ConnectorCreationRequest]
  }

  // TODO: remove this test after ohara manager starts to use new APIs
  @Test
  def testStaleCreationResponseApis(): Unit = {
    val desc = ConnectorDescription(
      id = CommonUtils.randomString(),
      settings = Map(
        ConnectorFormatter.NAME_KEY -> JsString(CommonUtils.randomString()),
        ConnectorFormatter.CLASS_NAME_KEY -> JsString(CommonUtils.randomString()),
        ConnectorFormatter.COLUMNS_KEY -> JsNull,
        ConnectorFormatter.TOPIC_NAMES_KEY -> JsArray(JsString(CommonUtils.randomString())),
        ConnectorFormatter.NUMBER_OF_TASKS_KEY -> JsNumber(1231),
        ConnectorFormatter.WORKER_CLUSTER_NAME_KEY -> JsString(CommonUtils.randomString()),
      ),
      state = None,
      error = None,
      lastModified = CommonUtils.current()
    )
    val jsonString = CONNECTOR_DESCRIPTION_JSON_FORMAT.write(desc).toString()
    jsonString.contains("id") shouldBe true
    jsonString.contains("name") shouldBe true
    jsonString.contains("className") shouldBe true
    jsonString.contains("schema") shouldBe true
    jsonString.contains("topics") shouldBe true
    jsonString.contains("numberOfTasks") shouldBe true
    jsonString.contains("settings") shouldBe true
    jsonString.contains("workerClusterName") shouldBe true
    jsonString.contains("state") shouldBe true
    jsonString.contains("error") shouldBe true
    jsonString.contains("lastModified") shouldBe true
    jsonString.contains("configs") shouldBe true
  }
}
