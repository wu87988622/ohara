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

package com.island.ohara.connector.jdbc.source

import com.island.ohara.client.kafka.WorkerClient
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.kafka.connector.json.SettingDefinition
import com.island.ohara.testing.WithBrokerWorker
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.duration._
import scala.collection.JavaConverters._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class TestJDBCSourceConnectorDefinitions extends WithBrokerWorker with Matchers {
  private[this] val jdbcSource = new JDBCSourceConnector
  private[this] val workerClient = WorkerClient(testUtil().workersConnProps())
  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)

  @Test
  def checkDbURL(): Unit = {
    val definition = jdbcSource.definitions().asScala.find(_.key() == DB_URL).get
    definition.required shouldBe true
    definition.defaultValue shouldBe null
    definition.editable() shouldBe true
    definition.internal() shouldBe false
    definition.reference() shouldBe "NONE"
    definition.valueType() shouldBe SettingDefinition.Type.STRING.name()
  }

  @Test
  def checkDbUserName(): Unit = {
    val definition = jdbcSource.definitions().asScala.find(_.key() == DB_USERNAME).get
    definition.required shouldBe true
    definition.defaultValue shouldBe null
    definition.editable() shouldBe true
    definition.internal() shouldBe false
    definition.reference() shouldBe "NONE"
    definition.valueType() shouldBe SettingDefinition.Type.STRING.name()
  }

  @Test
  def checkDbPassword(): Unit = {
    val definition = jdbcSource.definitions().asScala.find(_.key() == DB_PASSWORD).get
    definition.required shouldBe true
    definition.defaultValue shouldBe null
    definition.editable() shouldBe true
    definition.internal() shouldBe false
    definition.reference() shouldBe "NONE"
    definition.valueType() shouldBe SettingDefinition.Type.PASSWORD.name()
  }

  @Test
  def checkTableName(): Unit = {
    val definition = jdbcSource.definitions().asScala.find(_.key() == DB_TABLENAME).get
    definition.required shouldBe true
    definition.defaultValue shouldBe null
    definition.editable() shouldBe true
    definition.internal() shouldBe false
    definition.reference() shouldBe "NONE"
    definition.valueType() shouldBe SettingDefinition.Type.JDBC_TABLE.name()
  }

  @Test
  def checkCatalogPattern(): Unit = {
    val definition = jdbcSource.definitions().asScala.find(_.key() == DB_CATALOG_PATTERN).get
    definition.required shouldBe false
    definition.defaultValue shouldBe null
    definition.editable() shouldBe true
    definition.internal() shouldBe false
    definition.reference() shouldBe "NONE"
    definition.valueType() shouldBe SettingDefinition.Type.STRING.name()
  }

  @Test
  def checkSchemaPattern(): Unit = {
    val definition = jdbcSource.definitions().asScala.find(_.key() == DB_SCHEMA_PATTERN).get
    definition.required shouldBe false
    definition.defaultValue shouldBe null
    definition.editable() shouldBe true
    definition.internal() shouldBe false
    definition.reference() shouldBe "NONE"
    definition.valueType() shouldBe SettingDefinition.Type.STRING.name()
  }

  @Test
  def checkMode(): Unit = {
    val definition = jdbcSource.definitions().asScala.find(_.key() == MODE).get
    definition.required shouldBe false
    definition.defaultValue shouldBe MODE_DEFAULT
    definition.editable() shouldBe true
    definition.internal() shouldBe false
    definition.reference() shouldBe "NONE"
    definition.valueType() shouldBe SettingDefinition.Type.STRING.name()
  }

  @Test
  def checkTimeStampColumnName(): Unit = {
    val definition = jdbcSource.definitions().asScala.find(_.key() == TIMESTAMP_COLUMN_NAME).get
    definition.required shouldBe true
    definition.defaultValue shouldBe null
    definition.editable() shouldBe true
    definition.internal() shouldBe false
    definition.reference() shouldBe "NONE"
    definition.valueType() shouldBe SettingDefinition.Type.STRING.name()
  }

  @Test
  def testSource(): Unit = {
    val url: String = "jdbc:postgresql://localhost:5432/postgres"
    val userName: String = "user1"
    val password: String = "123456"
    val tableName: String = "table1"
    val timeStampColumnName: String = "COLUMN1"

    val response = result(
      workerClient
        .connectorValidator()
        .name(CommonUtils.randomString(10))
        .numberOfTasks(1)
        .topicName(CommonUtils.randomString(5))
        .settings(
          Map(DB_URL -> url,
              DB_USERNAME -> userName,
              DB_PASSWORD -> password,
              DB_TABLENAME -> tableName,
              TIMESTAMP_COLUMN_NAME -> timeStampColumnName))
        .connectorClass(classOf[JDBCSourceConnector])
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

    response.settings().asScala.filter(_.value().key() == DB_URL).head.definition().required() shouldBe true

    response.settings().asScala.filter(_.value().key() == DB_USERNAME).head.definition().required() shouldBe true

    response.settings().asScala.filter(_.value().key() == DB_PASSWORD).head.definition().required() shouldBe true

    response.settings().asScala.filter(_.value().key() == DB_TABLENAME).head.definition().required() shouldBe true

    response
      .settings()
      .asScala
      .filter(_.value().key() == TIMESTAMP_COLUMN_NAME)
      .head
      .definition()
      .required() shouldBe true

    response
      .settings()
      .asScala
      .filter(_.value().key() == DB_CATALOG_PATTERN)
      .head
      .definition()
      .required() shouldBe false

    response.settings().asScala.filter(_.value().key() == DB_SCHEMA_PATTERN).head.definition().required() shouldBe false

    response.errorCount() shouldBe 0
  }
}
