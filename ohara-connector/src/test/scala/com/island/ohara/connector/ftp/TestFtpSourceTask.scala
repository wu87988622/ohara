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

package com.island.ohara.connector.ftp

import java.util.Collections

import com.island.ohara.client.ftp.FtpClient
import com.island.ohara.common.exception.OharaException
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.setting.{ConnectorKey, TopicKey}
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.kafka.connector.TaskSetting
import com.island.ohara.kafka.connector.csv.CsvConnector
import com.island.ohara.kafka.connector.csv.source.CsvDataReader
import com.island.ohara.kafka.connector.json.ConnectorFormatter
import com.island.ohara.kafka.connector.storage.Storage
import com.island.ohara.testing.service.FtpServer
import org.junit.{After, Before, Test}
import org.scalatest.Matchers

import scala.collection.JavaConverters._

class TestFtpSourceTask extends SmallTest with Matchers {

  private[this] val ftpServer = FtpServer.builder().controlPort(0).dataPorts(java.util.Arrays.asList(0, 0, 0)).build()

  private[this] val props = FtpSourceProps(
    inputFolder = "/input",
    completedFolder = Some("/completed"),
    errorFolder = "/error",
    user = ftpServer.user,
    password = ftpServer.password,
    hostname = ftpServer.hostname,
    port = ftpServer.port,
    encode = "UTF-8"
  )

  private[this] val settings = props.toMap ++ Map(
    CsvConnector.TASK_TOTAL_CONFIG -> "1",
    CsvConnector.TASK_HASH_CONFIG -> "0"
  )

  @Before
  def setup(): Unit = {
    val ftpClient = createFtpClient();

    try {
      ftpClient.reMkdir(props.inputFolder)
      ftpClient.reMkdir(props.completedFolder.get)
      ftpClient.reMkdir(props.errorFolder)
    } finally ftpClient.close()
  }

  private[this] def createFtpClient() =
    FtpClient.builder().hostname(props.hostname).password(props.password).port(props.port).user(props.user).build()

  private[this] def createStorage(): Storage = {
    val task = createTask()
    val config = TaskSetting.of(settings.asJava)
    task._storage(config)
  }

  private[this] def createTask() = {
    val task = new FtpSourceTask()
    task.start(
      ConnectorFormatter
        .of()
        .connectorKey(ConnectorKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5)))
        .topicKey(TopicKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5)))
        .settings(settings.asJava)
        .raw())
    task
  }

  private[this] def createTask(settings: Map[String, String]) = {
    val task = new FtpSourceTask()
    task.start(
      ConnectorFormatter
        .of()
        .connectorKey(ConnectorKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5)))
        .topicKey(TopicKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5)))
        .settings(settings.asJava)
        .raw())
    task
  }

  @Test
  def testGetDataReader(): Unit = {
    val task = createTask(settings)
    task.getDataReader().getClass shouldBe classOf[CsvDataReader]
  }

  @Test
  def testGetDataReader_WithEmptyConfig(): Unit = {
    val settings = Map.empty[String, String]
    intercept[NoSuchElementException] {
      val task = createTask(settings)
      task.getDataReader()
    }
  }

  @Test
  def testStorage(): Unit = {
    val task = createTask()
    val config = TaskSetting.of(settings.asJava)
    task._storage(config) should not be (null)
  }

  @Test
  def testStorage_WithEmptyConfig(): Unit = {
    val task = createTask()
    intercept[NoSuchElementException] {
      task._storage(TaskSetting.of(Collections.emptyMap()))
    }
  }

  @Test
  def testListNonexistentInput(): Unit = {
    val ftpClient = createFtpClient()
    try ftpClient.delete(props.inputFolder)
    finally ftpClient.close()

    val storage = createStorage()
    // input folder doesn't exist should throw error
    intercept[OharaException] {
      storage.list(props.inputFolder).asScala.size shouldBe 0
    }
  }

  @Test
  def testListInput(): Unit = {
    val numberOfInputs = 3
    val ftpClient = createFtpClient()
    try {
      val data = (0 to 100).map(_.toString)
      (0 until numberOfInputs).foreach(index =>
        ftpClient.attach(CommonUtils.path(props.inputFolder, index.toString), data))
    } finally ftpClient.close()

    val storage = createStorage()
    storage.list(props.inputFolder).asScala.size shouldBe 3
  }

  @After
  def tearDown(): Unit = Releasable.close(ftpServer)
}
