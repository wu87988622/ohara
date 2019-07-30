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
import com.island.ohara.common.data.Cell
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.kafka.connector.TaskSetting
import com.island.ohara.kafka.connector.json.{ConnectorFormatter, ConnectorKey, TopicKey}
import com.island.ohara.kafka.connector.text.TextFileSystem
import com.island.ohara.kafka.connector.text.csv.CsvSourceConverterFactory
import com.island.ohara.testing.service.FtpServer
import org.junit.{After, Before, Test}
import org.scalatest.Matchers

import scala.collection.JavaConverters._

class TestFtpSourceTask extends SmallTest with Matchers {

  private[this] val ftpServer = FtpServer.builder().controlPort(0).dataPorts(java.util.Arrays.asList(0, 0, 0)).build()

  private[this] val props = FtpSourceTaskProps(
    hash = 0,
    total = 1,
    inputFolder = "/input",
    completedFolder = Some("/completed"),
    errorFolder = "/error",
    user = ftpServer.user,
    password = ftpServer.password,
    hostname = ftpServer.hostname,
    port = ftpServer.port,
    encode = "UTF-8"
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

  private[this] def setupInputData(path: String): Map[Int, Seq[Cell[String]]] = {
    val header = Seq("cf0", "cf1", "cf2")
    val line0 = Seq("a", "b", "c")
    val line1 = Seq("a", "d", "c")
    val line2 = Seq("a", "f", "c")
    val data =
      s"""${header.mkString(",")}
         |${line0.mkString(",")}
         |${line1.mkString(",")}
         |${line2.mkString(",")}""".stripMargin
    val ftpClient = createFtpClient()
    try {
      ftpClient.attach(path, data)
    } finally ftpClient.close()

    // start with 1 since the 0 is header
    Map(
      1 -> header.zipWithIndex.map {
        case (h, index) => Cell.of(h, line0(index))
      },
      2 -> header.zipWithIndex.map {
        case (h, index) => Cell.of(h, line1(index))
      },
      3 -> header.zipWithIndex.map {
        case (h, index) => Cell.of(h, line2(index))
      }
    )
  }

  private[this] def createTask() = {
    val task = new FtpSourceTask()
    task.start(
      ConnectorFormatter
        .of()
        .connectorKey(ConnectorKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5)))
        .topicKey(TopicKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5)))
        .settings(props.toMap.asJava)
        .raw())
    task
  }

  private[this] def createFileSystem(): TextFileSystem = {
    val task = createTask();
    val config = TaskSetting.of(props.toMap.asJava)
    task.getFileSystem(config)
  }

  private[this] def assertNumberOfFiles(numberOfInput: Int, numberOfCompleted: Int, numberOfError: Int) = {
    val ftpClient = createFtpClient()
    try {
      ftpClient.listFileNames(props.inputFolder).size shouldBe numberOfInput
      ftpClient.listFileNames(props.completedFolder.get).size shouldBe numberOfCompleted
      ftpClient.listFileNames(props.errorFolder).size shouldBe numberOfError
    } finally ftpClient.close()
  }

  @Test
  def testGetConverterFactory(): Unit = {
    val task = createTask()
    val config = TaskSetting.of(Map("topics" -> "T1").asJava)
    task.getConverterFactory(config).getClass shouldBe classOf[CsvSourceConverterFactory]
  }

  @Test
  def testGetConverterFactory_WithEmptyConfig(): Unit = {
    val task = createTask()
    intercept[NoSuchElementException] {
      task.getConverterFactory(TaskSetting.of(Collections.emptyMap()))
    }
  }

  @Test
  def testGetFileReader(): Unit = {
    val task = createTask()
    val config = TaskSetting.of(props.toMap.asJava)
    task.getFileSystem(config) should not be (null)
  }

  @Test
  def testGetFileReader_WithEmptyConfig(): Unit = {
    val task = createTask()
    intercept[NoSuchElementException] {
      task.getFileSystem(TaskSetting.of(Collections.emptyMap()))
    }
  }

  @Test
  def testListNonexistentInput(): Unit = {
    val ftpClient = createFtpClient()
    try ftpClient.delete(props.inputFolder)
    finally ftpClient.close()

    val fileSystem = createFileSystem()
    // input folder doesn't exist but no error is thrown.
    fileSystem.listInputFiles().size() shouldBe 0
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

    val fileSystem = createFileSystem()
    fileSystem.listInputFiles().size() shouldBe 3
  }

  @Test
  def testHandleCompletedFile(): Unit = {
    val path = CommonUtils.path(props.inputFolder, methodName)
    setupInputData(path)
    val fileSystem = createFileSystem()
    fileSystem.handleCompletedFile(path)
    assertNumberOfFiles(0, 1, 0)
  }

  @Test
  def testHandleErrorFile(): Unit = {
    val path = CommonUtils.path(props.inputFolder, methodName)
    setupInputData(path)
    val fileSystem = createFileSystem()
    fileSystem.handleErrorFile(path)
    assertNumberOfFiles(0, 0, 1)
  }

  @After
  def tearDown(): Unit = Releasable.close(ftpServer)
}
