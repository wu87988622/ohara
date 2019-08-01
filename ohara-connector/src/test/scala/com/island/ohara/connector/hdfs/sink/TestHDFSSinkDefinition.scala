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

package com.island.ohara.connector.hdfs.sink

import com.island.ohara.client.kafka.WorkerClient
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.kafka.connector.json.{ConnectorKey, SettingDefinition, TopicKey}
import com.island.ohara.testing.WithBrokerWorker
import org.junit.Test
import org.scalatest.Matchers

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class TestHDFSSinkDefinition extends WithBrokerWorker with Matchers {
  private[this] val hdfsSink = new HDFSSink
  private[this] val workerClient = WorkerClient(testUtil().workersConnProps())
  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)

  @Test
  def checkHdfsURL(): Unit = {
    val definition = hdfsSink.definitions().asScala.find(_.key() == HDFS_URL_CONFIG).get
    definition.required shouldBe true
    definition.defaultValue shouldBe null
    definition.editable() shouldBe true
    definition.internal() shouldBe false
    definition.reference() shouldBe "NONE"
    definition.valueType() shouldBe SettingDefinition.Type.STRING.name()
  }

  @Test
  def checkTopicsDir(): Unit = {
    val definition = hdfsSink.definitions().asScala.find(_.key() == TOPICS_DIR_CONFIG).get
    definition.required shouldBe false
    definition.defaultValue shouldBe TOPICS_DIR_DEFAULT.toString
    definition.editable() shouldBe true
    definition.internal() shouldBe false
    definition.reference() shouldBe "NONE"
    definition.valueType() shouldBe SettingDefinition.Type.STRING.name()
  }

  @Test
  def checkFlushSize(): Unit = {
    val definition = hdfsSink.definitions().asScala.find(_.key() == FLUSH_SIZE_CONFIG).get
    definition.required shouldBe false
    definition.defaultValue shouldBe FLUSH_SIZE_DEFAULT.toString
    definition.editable() shouldBe true
    definition.internal() shouldBe false
    definition.reference() shouldBe "NONE"
    definition.valueType() shouldBe SettingDefinition.Type.INT.name()
  }

  @Test
  def checkRotateIntervalMS(): Unit = {
    val definition = hdfsSink.definitions().asScala.find(_.key() == ROTATE_INTERVAL_MS_CONFIG).get
    definition.required shouldBe false
    definition.defaultValue shouldBe ROTATE_INTERVAL_MS_DEFAULT.toString
    definition.editable() shouldBe true
    definition.internal() shouldBe false
    definition.reference() shouldBe "NONE"
    definition.valueType() shouldBe SettingDefinition.Type.LONG.name()
  }

  @Test
  def checkFileNeedHeader(): Unit = {
    val definition = hdfsSink.definitions().asScala.find(_.key() == FILE_NEED_HEADER_CONFIG).get
    definition.required shouldBe false
    definition.defaultValue shouldBe FILE_NEED_HEADER_DEFAULT.toString
    definition.editable() shouldBe true
    definition.internal() shouldBe false
    definition.reference() shouldBe "NONE"
    definition.valueType() shouldBe SettingDefinition.Type.BOOLEAN.name()
  }

  @Test
  def checkFileEncode(): Unit = {
    val definition = hdfsSink.definitions().asScala.find(_.key() == FILE_ENCODE_CONFIG).get
    definition.required shouldBe false
    definition.defaultValue shouldBe FILE_ENCODE_DEFAULT.toString
    definition.editable() shouldBe true
    definition.internal() shouldBe false
    definition.reference() shouldBe "NONE"
    definition.valueType() shouldBe SettingDefinition.Type.STRING.name()
  }

  @Test
  def testNormal(): Unit = {
    val connectorKey = ConnectorKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    val topicKey = TopicKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    val response = result(
      workerClient
        .connectorValidator()
        .numberOfTasks(1)
        .connectorKey(connectorKey)
        .topicKey(topicKey)
        .settings(Map(
          HDFS_URL_CONFIG -> s"file://${testUtil.hdfs.tmpDirectory}",
          TOPICS_DIR_CONFIG -> s"file://${testUtil.hdfs.tmpDirectory}",
          FLUSH_SIZE_CONFIG -> "10",
          ROTATE_INTERVAL_MS_CONFIG -> "4000"
        ))
        .connectorClass(classOf[HDFSSink])
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
