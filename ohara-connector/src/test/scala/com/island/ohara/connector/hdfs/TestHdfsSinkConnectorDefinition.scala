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

package com.island.ohara.connector.hdfs

import com.island.ohara.client.kafka.WorkerClient
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.kafka.connector.json.{SettingDefinition, TopicKey}
import com.island.ohara.testing.WithBrokerWorker
import org.junit.Test
import org.scalatest.Matchers

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
class TestHdfsSinkConnectorDefinition extends WithBrokerWorker with Matchers {
  private[this] val hdfsSinkConnector = new HDFSSinkConnector
  private[this] val workerClient = WorkerClient(testUtil().workersConnProps())
  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)

  @Test
  def checkHdfsURL(): Unit = {
    val definition = hdfsSinkConnector.definitions().asScala.find(_.key() == HDFS_URL).get
    definition.required shouldBe true
    definition.defaultValue shouldBe null
    definition.editable() shouldBe true
    definition.internal() shouldBe false
    definition.reference() shouldBe "NONE"
    definition.valueType() shouldBe SettingDefinition.Type.STRING.name()
  }

  @Test
  def checkFlushLineCount(): Unit = {
    val definition = hdfsSinkConnector.definitions().asScala.find(_.key() == FLUSH_LINE_COUNT).get
    definition.required shouldBe false
    definition.defaultValue shouldBe HDFSSinkConnectorConfig.FLUSH_LINE_COUNT_DEFAULT.toString
    definition.editable() shouldBe true
    definition.internal() shouldBe false
    definition.reference() shouldBe "NONE"
    definition.valueType() shouldBe SettingDefinition.Type.INT.name()
  }

  @Test
  def checkRotateIntervalMS(): Unit = {
    val definition = hdfsSinkConnector.definitions().asScala.find(_.key() == ROTATE_INTERVAL_MS).get
    definition.required shouldBe false
    definition.defaultValue shouldBe HDFSSinkConnectorConfig.ROTATE_INTERVAL_MS_DEFAULT.toString
    definition.editable() shouldBe true
    definition.internal() shouldBe false
    definition.reference() shouldBe "NONE"
    definition.valueType() shouldBe SettingDefinition.Type.LONG.name()
  }

  @Test
  def checkTmpDir(): Unit = {
    val definition = hdfsSinkConnector.definitions().asScala.find(_.key() == TMP_DIR).get
    definition.required shouldBe false
    definition.defaultValue shouldBe HDFSSinkConnectorConfig.TMP_DIR_DEFAULT.toString
    definition.editable() shouldBe true
    definition.internal() shouldBe false
    definition.reference() shouldBe "NONE"
    definition.valueType() shouldBe SettingDefinition.Type.STRING.name()
  }

  @Test
  def checkDataDir(): Unit = {
    val definition = hdfsSinkConnector.definitions().asScala.find(_.key() == DATA_DIR).get
    definition.required shouldBe false
    definition.defaultValue shouldBe HDFSSinkConnectorConfig.DATA_DIR_DEFAULT.toString
    definition.editable() shouldBe true
    definition.internal() shouldBe false
    definition.reference() shouldBe "NONE"
    definition.valueType() shouldBe SettingDefinition.Type.STRING.name()
  }

  @Test
  def checkDataFilePrefixName(): Unit = {
    val definition = hdfsSinkConnector.definitions().asScala.find(_.key() == DATAFILE_PREFIX_NAME).get
    definition.required shouldBe false
    definition.defaultValue shouldBe HDFSSinkConnectorConfig.DATAFILE_PREFIX_NAME_DEFAULT.toString
    definition.editable() shouldBe true
    definition.internal() shouldBe false
    definition.reference() shouldBe "NONE"
    definition.valueType() shouldBe SettingDefinition.Type.STRING.name()
  }

  @Test
  def checkDataFileNeedHeader(): Unit = {
    val definition = hdfsSinkConnector.definitions().asScala.find(_.key() == DATAFILE_NEEDHEADER).get
    definition.required shouldBe false
    definition.defaultValue shouldBe HDFSSinkConnectorConfig.DATAFILE_NEEDHEADER_DEFAULT.toString
    definition.editable() shouldBe true
    definition.internal() shouldBe false
    definition.reference() shouldBe "NONE"
    definition.valueType() shouldBe SettingDefinition.Type.BOOLEAN.name()
  }

  @Test
  def testSink1(): Unit = {
    val topicKey = TopicKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    val response = result(
      workerClient
        .connectorValidator()
        .name(CommonUtils.randomString())
        .numberOfTasks(1)
        .topicKey(topicKey)
        .settings(Map(HDFS_URL -> s"file://${testUtil.hdfs.tmpDirectory}"))
        .connectorClass(classOf[HDFSSinkConnector])
        .run())

    response.settings().size should not be 0
    response
      .settings()
      .asScala
      .filter(_.definition().key() == SettingDefinition.TOPIC_NAMES_DEFINITION.key())
      .head
      .definition()
      .required() shouldBe true
    response
      .settings()
      .asScala
      .filter(_.definition().key() == SettingDefinition.CONNECTOR_CLASS_DEFINITION.key())
      .head
      .definition()
      .required() shouldBe true
    response
      .settings()
      .asScala
      .filter(_.definition().key() == SettingDefinition.NUMBER_OF_TASKS_DEFINITION.key())
      .head
      .definition()
      .required() shouldBe true
    response
      .settings()
      .asScala
      .filter(_.definition().key() == SettingDefinition.COLUMNS_DEFINITION.key())
      .head
      .definition()
      .required() shouldBe false
    response
      .settings()
      .asScala
      .filter(_.definition().key() == SettingDefinition.WORKER_CLUSTER_NAME_DEFINITION.key())
      .head
      .definition()
      .required() shouldBe false
    response.errorCount() shouldBe 0
  }

  @Test
  def testSink2(): Unit = {
    val topicKey = TopicKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    val response = result(
      workerClient
        .connectorValidator()
        .name(CommonUtils.randomString())
        .numberOfTasks(1)
        .topicKey(topicKey)
        .settings(Map(FLUSH_LINE_COUNT -> "1000", TMP_DIR -> "/tmp", HDFS_URL -> "file://"))
        .connectorClass(classOf[HDFSSinkConnector])
        .run())

    response.settings().size should not be 0
    response
      .settings()
      .asScala
      .filter(_.definition().key() == SettingDefinition.TOPIC_NAMES_DEFINITION.key())
      .head
      .definition()
      .required() shouldBe true
    response
      .settings()
      .asScala
      .filter(_.definition().key() == SettingDefinition.CONNECTOR_CLASS_DEFINITION.key())
      .head
      .definition()
      .required() shouldBe true
    response
      .settings()
      .asScala
      .filter(_.definition().key() == SettingDefinition.NUMBER_OF_TASKS_DEFINITION.key())
      .head
      .definition()
      .required() shouldBe true
    response
      .settings()
      .asScala
      .filter(_.definition().key() == SettingDefinition.COLUMNS_DEFINITION.key())
      .head
      .definition()
      .required() shouldBe false
    response
      .settings()
      .asScala
      .filter(_.definition().key() == SettingDefinition.WORKER_CLUSTER_NAME_DEFINITION.key())
      .head
      .definition()
      .required() shouldBe false
    response.errorCount() shouldBe 0
  }
}
