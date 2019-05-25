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

import java.util.Collections

import com.island.ohara.client.configurator.v0.ConnectorApi.ConnectorState
import com.island.ohara.common.data.{Row, Serializer}
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.kafka.Consumer
import com.island.ohara.kafka.connector.json.{ConverterType, SettingDefinition, StringList}
import com.island.ohara.testing.With3Brokers3Workers
import org.junit.Test
import org.scalatest.Matchers

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
class TestWorkerClient extends With3Brokers3Workers with Matchers {

  private[this] val workerClient = WorkerClient(testUtil().workersConnProps())
  @Test
  def testExist(): Unit = {
    val topicName = CommonUtils.randomString(10)
    val connectorName = CommonUtils.randomString(10)
    result(workerClient.exist(connectorName)) shouldBe false

    result(
      workerClient
        .connectorCreator()
        .topicName(topicName)
        .connectorClass(classOf[MyConnector])
        .id(connectorName)
        .numberOfTasks(1)
        .create)

    try assertExist(workerClient, connectorName)
    finally result(workerClient.delete(connectorName))
  }

  @Test
  def testExistOnUnrunnableConnector(): Unit = {
    val topicName = CommonUtils.randomString(10)
    val connectorName = CommonUtils.randomString(10)
    result(workerClient.exist(connectorName)) shouldBe false

    result(
      workerClient
        .connectorCreator()
        .topicName(topicName)
        .connectorClass(classOf[BrokenConnector])
        .id(connectorName)
        .numberOfTasks(1)
        .create)

    try assertExist(workerClient, connectorName)
    finally result(workerClient.delete(connectorName))
  }

  @Test
  def testPauseAndResumeSource(): Unit = {
    val topicName = CommonUtils.randomString(10)
    val connectorName = CommonUtils.randomString(10)
    result(
      workerClient
        .connectorCreator()
        .topicName(topicName)
        .connectorClass(classOf[MyConnector])
        .id(connectorName)
        .numberOfTasks(1)
        .create)
    try {
      assertExist(workerClient, connectorName)
      val consumer =
        Consumer
          .builder[Row, Array[Byte]]()
          .topicName(topicName)
          .offsetFromBegin()
          .connectionProps(testUtil.brokersConnProps)
          .keySerializer(Serializer.ROW)
          .valueSerializer(Serializer.BYTES)
          .build()
      try {
        // try to receive some data from topic
        var rows = consumer.poll(java.time.Duration.ofSeconds(10), 1)
        rows.size should not be 0
        rows.asScala.foreach(_.key.get shouldBe ROW)
        // pause connector
        result(workerClient.pause(connectorName))

        await(() => result(workerClient.status(connectorName)).connector.state == ConnectorState.PAUSED)

        // try to receive all data from topic...10 seconds should be enough in this case
        rows = consumer.poll(java.time.Duration.ofSeconds(10), Int.MaxValue)
        rows.asScala.foreach(_.key.get shouldBe ROW)

        // connector is paused so there is no data
        rows = consumer.poll(java.time.Duration.ofSeconds(20), 1)
        rows.size shouldBe 0

        // resume connector
        result(workerClient.resume(connectorName))

        await(() => result(workerClient.status(connectorName)).connector.state == ConnectorState.RUNNING)

        // since connector is resumed so some data are generated
        rows = consumer.poll(java.time.Duration.ofSeconds(20), 1)
        rows.size should not be 0
      } finally consumer.close()
    } finally result(workerClient.delete(connectorName))
  }

  @Test
  def testValidate(): Unit = {
    val name = CommonUtils.randomString(10)
    val topicName = CommonUtils.randomString(10)
    val numberOfTasks = 1
    val settingInfo = result(
      workerClient
        .connectorValidator()
        .className(classOf[MyConnector].getName)
        .settings(Map(
          SettingDefinition.CONNECTOR_ID_DEFINITION.key() -> name,
          SettingDefinition.TOPIC_NAMES_DEFINITION.key() -> StringList.toJsonString(
            Collections.singletonList(topicName)),
          SettingDefinition.NUMBER_OF_TASKS_DEFINITION.key() -> numberOfTasks.toString
        ))
        .run())
    settingInfo.className.get shouldBe classOf[MyConnector].getName
    settingInfo.settings.size should not be 0
    settingInfo.topicNames.asScala shouldBe Seq(topicName)
    settingInfo.numberOfTasks.get shouldBe numberOfTasks
  }

  @Test
  def ignoreTopicNames(): Unit = an[NoSuchElementException] should be thrownBy result(
    workerClient.connectorValidator().className(classOf[MyConnector].getName).run())

  @Test
  def ignoreClassName(): Unit =
    an[NoSuchElementException] should be thrownBy result(workerClient.connectorValidator().run())

  @Test
  def testValidateWithoutValue(): Unit = {
    val settingInfo = result(
      workerClient
        .connectorValidator()
        .className(classOf[MyConnector].getName)
        .topicName(CommonUtils.randomString())
        .run())
    settingInfo.className.get shouldBe classOf[MyConnector].getName
    settingInfo.settings.size should not be 0
    settingInfo.topicNames.isEmpty shouldBe false
    settingInfo.numberOfTasks.isPresent shouldBe false
  }

  @Test
  def testColumnsDefinition(): Unit =
    result(workerClient.connectors)
      .map(_.definitions.filter(_.key() == SettingDefinition.COLUMNS_DEFINITION.key()).head)
      .foreach { definition =>
        definition.tableKeys().size() should not be 0
      }

  @Test
  def testAllPluginDefinitions(): Unit = {
    val plugins = result(workerClient.connectors)
    plugins.size should not be 0
    plugins.foreach(plugin => check(plugin.definitions))
  }
  @Test
  def testListDefinitions(): Unit = {
    check(result(workerClient.definitions(classOf[MyConnector].getName)))
  }

  private[this] def check(settingDefinitionS: Seq[SettingDefinition]): Unit = {
    settingDefinitionS.size should not be 0

    settingDefinitionS.exists(_.key() == SettingDefinition.CONNECTOR_CLASS_DEFINITION.key()) shouldBe true
    settingDefinitionS
      .find(_.key() == SettingDefinition.CONNECTOR_CLASS_DEFINITION.key())
      .head
      .group() shouldBe SettingDefinition.CORE_GROUP
    settingDefinitionS
      .find(_.key() == SettingDefinition.CONNECTOR_CLASS_DEFINITION.key())
      .head
      .internal() shouldBe false
    settingDefinitionS.find(_.key() == SettingDefinition.CONNECTOR_CLASS_DEFINITION.key()).head.editable() shouldBe true
    settingDefinitionS
      .find(_.key() == SettingDefinition.CONNECTOR_CLASS_DEFINITION.key())
      .head
      .defaultValue() shouldBe null

    settingDefinitionS.exists(_.key() == SettingDefinition.COLUMNS_DEFINITION.key()) shouldBe true
    settingDefinitionS
      .find(_.key() == SettingDefinition.COLUMNS_DEFINITION.key())
      .head
      .group() shouldBe SettingDefinition.CORE_GROUP
    settingDefinitionS.find(_.key() == SettingDefinition.COLUMNS_DEFINITION.key()).head.internal() shouldBe false
    settingDefinitionS.find(_.key() == SettingDefinition.COLUMNS_DEFINITION.key()).head.editable() shouldBe true
    settingDefinitionS.find(_.key() == SettingDefinition.COLUMNS_DEFINITION.key()).head.defaultValue() shouldBe null

    settingDefinitionS.exists(_.key() == SettingDefinition.NUMBER_OF_TASKS_DEFINITION.key()) shouldBe true
    settingDefinitionS
      .find(_.key() == SettingDefinition.NUMBER_OF_TASKS_DEFINITION.key())
      .head
      .group() shouldBe SettingDefinition.CORE_GROUP
    settingDefinitionS
      .find(_.key() == SettingDefinition.NUMBER_OF_TASKS_DEFINITION.key())
      .head
      .internal() shouldBe false
    settingDefinitionS.find(_.key() == SettingDefinition.NUMBER_OF_TASKS_DEFINITION.key()).head.editable() shouldBe true
    settingDefinitionS
      .find(_.key() == SettingDefinition.NUMBER_OF_TASKS_DEFINITION.key())
      .head
      .defaultValue() shouldBe null

    settingDefinitionS.exists(_.key() == SettingDefinition.TOPIC_NAMES_DEFINITION.key()) shouldBe true
    settingDefinitionS
      .find(_.key() == SettingDefinition.TOPIC_NAMES_DEFINITION.key())
      .head
      .group() shouldBe SettingDefinition.CORE_GROUP
    settingDefinitionS.find(_.key() == SettingDefinition.TOPIC_NAMES_DEFINITION.key()).head.internal() shouldBe false
    settingDefinitionS.find(_.key() == SettingDefinition.TOPIC_NAMES_DEFINITION.key()).head.editable() shouldBe true
    settingDefinitionS.find(_.key() == SettingDefinition.TOPIC_NAMES_DEFINITION.key()).head.defaultValue() shouldBe null

    settingDefinitionS.exists(_.key() == SettingDefinition.WORKER_CLUSTER_NAME_DEFINITION.key()) shouldBe true
    settingDefinitionS
      .find(_.key() == SettingDefinition.WORKER_CLUSTER_NAME_DEFINITION.key())
      .head
      .group() shouldBe SettingDefinition.CORE_GROUP
    settingDefinitionS
      .find(_.key() == SettingDefinition.WORKER_CLUSTER_NAME_DEFINITION.key())
      .head
      .internal() shouldBe false
    settingDefinitionS
      .find(_.key() == SettingDefinition.WORKER_CLUSTER_NAME_DEFINITION.key())
      .head
      .editable() shouldBe true
    settingDefinitionS
      .find(_.key() == SettingDefinition.WORKER_CLUSTER_NAME_DEFINITION.key())
      .head
      .defaultValue() shouldBe null

    settingDefinitionS.exists(_.key() == SettingDefinition.AUTHOR_DEFINITION.key()) shouldBe true
    settingDefinitionS
      .find(_.key() == SettingDefinition.AUTHOR_DEFINITION.key())
      .head
      .group() shouldBe SettingDefinition.CORE_GROUP
    settingDefinitionS.find(_.key() == SettingDefinition.AUTHOR_DEFINITION.key()).head.internal() shouldBe false
    settingDefinitionS.find(_.key() == SettingDefinition.AUTHOR_DEFINITION.key()).head.editable() shouldBe false
    settingDefinitionS.find(_.key() == SettingDefinition.AUTHOR_DEFINITION.key()).head.defaultValue() shouldBe "unknown"

    settingDefinitionS.exists(_.key() == SettingDefinition.VERSION_DEFINITION.key()) shouldBe true
    settingDefinitionS
      .find(_.key() == SettingDefinition.VERSION_DEFINITION.key())
      .head
      .group() shouldBe SettingDefinition.CORE_GROUP
    settingDefinitionS.find(_.key() == SettingDefinition.VERSION_DEFINITION.key()).head.internal() shouldBe false
    settingDefinitionS.find(_.key() == SettingDefinition.VERSION_DEFINITION.key()).head.editable() shouldBe false
    settingDefinitionS
      .find(_.key() == SettingDefinition.VERSION_DEFINITION.key())
      .head
      .defaultValue() shouldBe "unknown"

    settingDefinitionS.exists(_.key() == SettingDefinition.REVISION_DEFINITION.key()) shouldBe true
    settingDefinitionS
      .find(_.key() == SettingDefinition.REVISION_DEFINITION.key())
      .head
      .group() shouldBe SettingDefinition.CORE_GROUP
    settingDefinitionS.find(_.key() == SettingDefinition.REVISION_DEFINITION.key()).head.internal() shouldBe false
    settingDefinitionS.find(_.key() == SettingDefinition.REVISION_DEFINITION.key()).head.editable() shouldBe false
    settingDefinitionS
      .find(_.key() == SettingDefinition.REVISION_DEFINITION.key())
      .head
      .defaultValue() shouldBe "unknown"

    settingDefinitionS.exists(_.key() == SettingDefinition.KIND_DEFINITION.key()) shouldBe true
    settingDefinitionS
      .find(_.key() == SettingDefinition.KIND_DEFINITION.key())
      .head
      .group() shouldBe SettingDefinition.CORE_GROUP
    settingDefinitionS.find(_.key() == SettingDefinition.KIND_DEFINITION.key()).head.internal() shouldBe false
    settingDefinitionS.find(_.key() == SettingDefinition.KIND_DEFINITION.key()).head.editable() shouldBe false
    (settingDefinitionS.find(_.key() == SettingDefinition.KIND_DEFINITION.key()).head.defaultValue() == "source"
    || settingDefinitionS
      .find(_.key() == SettingDefinition.KIND_DEFINITION.key())
      .head
      .defaultValue() == "sink") shouldBe true

    settingDefinitionS.exists(_.key() == SettingDefinition.KEY_CONVERTER_DEFINITION.key()) shouldBe true
    settingDefinitionS
      .find(_.key() == SettingDefinition.KEY_CONVERTER_DEFINITION.key())
      .head
      .group() shouldBe SettingDefinition.CORE_GROUP
    settingDefinitionS.find(_.key() == SettingDefinition.KEY_CONVERTER_DEFINITION.key()).head.internal() shouldBe true
    settingDefinitionS.find(_.key() == SettingDefinition.KEY_CONVERTER_DEFINITION.key()).head.editable() shouldBe true
    settingDefinitionS
      .find(_.key() == SettingDefinition.KEY_CONVERTER_DEFINITION.key())
      .head
      .defaultValue() shouldBe ConverterType.NONE.className()

    settingDefinitionS.exists(_.key() == SettingDefinition.VALUE_CONVERTER_DEFINITION.key()) shouldBe true
    settingDefinitionS
      .find(_.key() == SettingDefinition.VALUE_CONVERTER_DEFINITION.key())
      .head
      .group() shouldBe SettingDefinition.CORE_GROUP
    settingDefinitionS.find(_.key() == SettingDefinition.VALUE_CONVERTER_DEFINITION.key()).head.internal() shouldBe true
    settingDefinitionS.find(_.key() == SettingDefinition.VALUE_CONVERTER_DEFINITION.key()).head.editable() shouldBe true
    settingDefinitionS
      .find(_.key() == SettingDefinition.VALUE_CONVERTER_DEFINITION.key())
      .head
      .defaultValue() shouldBe ConverterType.NONE.className()

    settingDefinitionS.exists(_.key() == SettingDefinition.CONNECTOR_NAME_DEFINITION.key()) shouldBe true
    settingDefinitionS
      .find(_.key() == SettingDefinition.CONNECTOR_NAME_DEFINITION.key())
      .head
      .group() shouldBe SettingDefinition.CORE_GROUP
    settingDefinitionS.find(_.key() == SettingDefinition.CONNECTOR_NAME_DEFINITION.key()).head.internal() shouldBe false
    settingDefinitionS.find(_.key() == SettingDefinition.CONNECTOR_NAME_DEFINITION.key()).head.editable() shouldBe true
    settingDefinitionS
      .find(_.key() == SettingDefinition.CONNECTOR_NAME_DEFINITION.key())
      .head
      .defaultValue() shouldBe null
  }

  @Test
  def passIncorrectColumns(): Unit = {
    val e = intercept[IllegalArgumentException] {
      result(
        workerClient
          .connectorCreator()
          .topicName(CommonUtils.randomString(10))
          .connectorClass(classOf[MyConnector])
          .id(CommonUtils.randomString(10))
          .numberOfTasks(1)
          .settings(Map(SettingDefinition.COLUMNS_DEFINITION.key() -> "Asdasdasd"))
          .create)
    }
    //see SettingDefinition.validator
    e.getMessage.contains("can't be converted to PropGroups type") shouldBe true
  }

  @Test
  def passIncorrectDuration(): Unit = {
    val e = intercept[IllegalArgumentException] {
      result(
        workerClient
          .connectorCreator()
          .topicName(CommonUtils.randomString(10))
          .connectorClass(classOf[MyConnector])
          .id(CommonUtils.randomString(10))
          .numberOfTasks(1)
          .settings(Map(MyConnector.DURATION_KEY -> "Asdasdasd"))
          .create)
    }
    //see SettingDefinition.validator
    e.getMessage.contains("can't be converted to Duration type") shouldBe true
  }

  @Test
  def pass1Second(): Unit = {
    result(
      workerClient
        .connectorCreator()
        .topicName(CommonUtils.randomString(10))
        .connectorClass(classOf[MyConnector])
        .id(CommonUtils.randomString(10))
        .numberOfTasks(1)
        .settings(Map(MyConnector.DURATION_KEY -> "PT1S"))
        .create)
  }
  @Test
  def pass1Minute1Second(): Unit = {
    result(
      workerClient
        .connectorCreator()
        .topicName(CommonUtils.randomString(10))
        .connectorClass(classOf[MyConnector])
        .id(CommonUtils.randomString(10))
        .numberOfTasks(1)
        .settings(Map(MyConnector.DURATION_KEY -> "PT1M1S"))
        .create)
  }
}
