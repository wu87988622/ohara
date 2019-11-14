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

import java.io.FileOutputStream

import com.island.ohara.client.configurator.v0.InspectApi.{RdbColumn, RdbInfo}
import com.island.ohara.client.configurator.v0.{
  BrokerApi,
  FileInfoApi,
  InspectApi,
  StreamApi,
  TopicApi,
  WorkerApi,
  ZookeeperApi
}
import com.island.ohara.client.database.DatabaseClient
import com.island.ohara.common.rule.OharaTest
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.common.util.{CommonUtils, Releasable, VersionUtils}
import com.island.ohara.configurator.Configurator.Mode
import com.island.ohara.configurator.{Configurator, ReflectionUtils}
import com.island.ohara.testing.service.Database
import org.junit.{After, Test}
import org.scalatest.Matchers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TestInspectRoute extends OharaTest {
  private[this] val db = Database.local()
  private[this] val configurator = Configurator.builder.fake().build()

  private[this] def result[T](f: Future[T]): T = Await.result(f, 30 seconds)

  private[this] val workerClusterInfo = result(
    WorkerApi.access.hostname(configurator.hostname).port(configurator.port).list()).head

  private[this] def inspectApi = InspectApi.access.hostname(configurator.hostname).port(configurator.port)

  @Test
  def testQueryDb(): Unit = {
    val tableName = CommonUtils.randomString(10)
    val dbClient = DatabaseClient.builder.url(db.url()).user(db.user()).password(db.password()).build
    try {
      val r = result(
        inspectApi.rdbRequest
          .jdbcUrl(db.url())
          .user(db.user())
          .password(db.password())
          .workerClusterKey(workerClusterInfo.key)
          .query())
      r.name shouldBe "mysql"
      r.tables.isEmpty shouldBe true

      val cf0 = RdbColumn("cf0", "INTEGER", true)
      val cf1 = RdbColumn("cf1", "INTEGER", false)
      def verify(info: RdbInfo): Unit = {
        info.tables.count(_.name == tableName) shouldBe 1
        val table = info.tables.filter(_.name == tableName).head
        table.columns.size shouldBe 2
        table.columns.count(_.name == cf0.name) shouldBe 1
        table.columns.filter(_.name == cf0.name).head.pk shouldBe cf0.pk
        table.columns.count(_.name == cf1.name) shouldBe 1
        table.columns.filter(_.name == cf1.name).head.pk shouldBe cf1.pk
      }
      dbClient.createTable(tableName, Seq(cf0, cf1))

      verify(
        result(
          inspectApi.rdbRequest
            .jdbcUrl(db.url())
            .user(db.user())
            .password(db.password())
            .workerClusterKey(workerClusterInfo.key)
            .query()))

      verify(
        result(
          inspectApi.rdbRequest
            .jdbcUrl(db.url())
            .user(db.user())
            .password(db.password())
            .catalogPattern(db.databaseName)
            .tableName(tableName)
            .workerClusterKey(workerClusterInfo.key)
            .query()))
      dbClient.dropTable(tableName)
    } finally dbClient.close()
  }

  @Test
  def testQueryStreamFile(): Unit = {
    val streamFile = RouteUtils.streamFile
    streamFile.exists() shouldBe true
    def fileApi = FileInfoApi.access.hostname(configurator.hostname).port(configurator.port)
    val fileInfo = result(fileApi.request.file(streamFile).upload())

    val fileContent = result(inspectApi.fileRequest.key(fileInfo.key).query())
    fileContent.classes should not be Seq.empty
    fileContent.sourceConnectorClasses.size shouldBe 0
    fileContent.sinkConnectorClasses.size shouldBe 0
    fileContent.streamAppClasses.size shouldBe 1
  }

  @Test
  def testQueryConnectorFile(): Unit = {
    val connectorFile = RouteUtils.connectorFile
    connectorFile.exists() shouldBe true
    def fileApi = FileInfoApi.access.hostname(configurator.hostname).port(configurator.port)
    val fileInfo = result(fileApi.request.file(connectorFile).upload())

    val fileContent = result(inspectApi.fileRequest.key(fileInfo.key).query())
    fileContent.classes should not be Seq.empty
    fileContent.sourceConnectorClasses.size should not be 0
    fileContent.sourceConnectorClasses.foreach(d => d.settingDefinitions should not be Seq.empty)
    fileContent.sinkConnectorClasses.size should not be 0
    fileContent.sinkConnectorClasses.foreach(d => d.settingDefinitions should not be Seq.empty)
    fileContent.streamAppClasses.size shouldBe 0
  }

  @Test
  def testQueryIllegalFile(): Unit = {
    val file = {
      val f = CommonUtils.createTempFile(CommonUtils.randomString(10), ".jar")
      val output = new FileOutputStream(f)
      try output.write("asdasdsad".getBytes())
      finally output.close()
      f
    }
    def fileApi = FileInfoApi.access.hostname(configurator.hostname).port(configurator.port)
    val fileInfo = result(fileApi.request.file(file).upload())

    val fileContent = result(inspectApi.fileRequest.key(fileInfo.key).query())
    fileContent.classes shouldBe Seq.empty
  }

  @Test
  def testConfiguratorInfo(): Unit = {
    // only test the configurator based on mini cluster
    val clusterInformation = result(inspectApi.configuratorInfo())
    clusterInformation.versionInfo.version shouldBe VersionUtils.VERSION
    clusterInformation.versionInfo.branch shouldBe VersionUtils.BRANCH
    clusterInformation.versionInfo.user shouldBe VersionUtils.USER
    clusterInformation.versionInfo.revision shouldBe VersionUtils.REVISION
    clusterInformation.versionInfo.date shouldBe VersionUtils.DATE
    clusterInformation.mode shouldBe Mode.FAKE.toString
  }

  @Test
  def testZookeeperInfo(): Unit = {
    val info = result(inspectApi.zookeeperInfo())
    info.imageName shouldBe ZookeeperApi.IMAGE_NAME_DEFAULT
    info.settingDefinitions.size shouldBe ZookeeperApi.DEFINITIONS.size
    info.settingDefinitions.foreach { definition =>
      definition shouldBe ZookeeperApi.DEFINITIONS.find(_.key() == definition.key()).get
    }
    info.classInfos shouldBe Seq.empty
  }

  @Test
  def testBrokerInfo(): Unit = {
    val info = result(inspectApi.brokerInfo())
    info.imageName shouldBe BrokerApi.IMAGE_NAME_DEFAULT
    info.settingDefinitions.size shouldBe BrokerApi.DEFINITIONS.size
    info.settingDefinitions.foreach { definition =>
      definition shouldBe BrokerApi.DEFINITIONS.find(_.key() == definition.key()).get
    }
    info.classInfos.size shouldBe 1
    info.classInfos.head.classType shouldBe "topic"
    info.classInfos.head.settingDefinitions.size shouldBe TopicApi.DEFINITIONS.size
  }

  @Test
  def testWorkerInfo(): Unit = {
    val info = result(inspectApi.workerInfo())
    info.imageName shouldBe WorkerApi.IMAGE_NAME_DEFAULT
    info.settingDefinitions.size shouldBe WorkerApi.DEFINITIONS.size
    info.settingDefinitions.foreach { definition =>
      definition shouldBe WorkerApi.DEFINITIONS.find(_.key() == definition.key()).get
    }
    info.classInfos shouldBe Seq.empty
  }

  @Test
  def testWorkerInfoWithKey(): Unit = {
    val info = result(inspectApi.workerInfo(workerClusterInfo.key))
    info.imageName shouldBe WorkerApi.IMAGE_NAME_DEFAULT
    info.settingDefinitions.size shouldBe WorkerApi.DEFINITIONS.size
    info.settingDefinitions.foreach { definition =>
      definition shouldBe WorkerApi.DEFINITIONS.find(_.key() == definition.key()).get
    }
    info.classInfos.size shouldBe ReflectionUtils.localConnectorDefinitions.size
  }

  @Test
  def testStreamInfo(): Unit = {
    val info = result(inspectApi.streamInfo())
    info.imageName shouldBe StreamApi.IMAGE_NAME_DEFAULT
    // the jar is empty but we still see the default definitions
    info.settingDefinitions should not be Seq.empty
    info.classInfos shouldBe Seq.empty
  }

  @Test
  def testStreamInfoWithKey(): Unit = {
    val info = result(inspectApi.streamInfo(ObjectKey.of("g", "n")))
    info.imageName shouldBe StreamApi.IMAGE_NAME_DEFAULT
    // the jar is empty but we still see the default definitions
    info.settingDefinitions should not be Seq.empty
    info.classInfos shouldBe Seq.empty
  }

  @After
  def tearDown(): Unit = {
    Releasable.close(configurator)
    Releasable.close(db)
  }
}
