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
import com.island.ohara.client.FtpClient
import com.island.ohara.common.data.{Cell, Column, DataType, Row}
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.{CommonUtil, Releasable}
import com.island.ohara.kafka.connector.{RowSourceContext, TaskConfig}
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
    completedFolder = "/completed",
    errorFolder = "/error",
    user = ftpServer.user,
    password = ftpServer.password,
    hostname = ftpServer.hostname,
    port = ftpServer.port,
    encode = Some("UTF-8")
  )

  @Before
  def setup(): Unit = {
    val ftpClient = FtpClient
      .builder()
      .hostname(ftpServer.hostname)
      .password(ftpServer.password)
      .port(ftpServer.port)
      .user(ftpServer.user)
      .build()

    try {
      ftpClient.reMkdir(props.inputFolder)
      ftpClient.reMkdir(props.completedFolder.get)
      ftpClient.reMkdir(props.errorFolder)
    } finally ftpClient.close()
  }

  private[this] def createTask() = {
    val task = new FtpSourceTask()

    task._start(TaskConfig.builder().name(methodName()).options(props.toMap.asJava).build())
    task
  }

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
    val ftpClient = FtpClient
      .builder()
      .hostname(ftpServer.hostname)
      .password(ftpServer.password)
      .port(ftpServer.port)
      .user(ftpServer.user)
      .build()
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

  private[this] def assertNumberOfFiles(numberOfInput: Int, numberOfCompleted: Int, numberOfError: Int) = {
    val ftpClient = FtpClient
      .builder()
      .hostname(ftpServer.hostname)
      .password(ftpServer.password)
      .port(ftpServer.port)
      .user(ftpServer.user)
      .build()
    try {
      ftpClient.listFileNames(props.inputFolder).size shouldBe numberOfInput
      ftpClient.listFileNames(props.completedFolder.get).size shouldBe numberOfCompleted
      ftpClient.listFileNames(props.errorFolder).size shouldBe numberOfError
    } finally ftpClient.close()
  }

  @Test
  def testListNonexistentInput(): Unit = {
    val ftpClient = FtpClient
      .builder()
      .hostname(ftpServer.hostname)
      .password(ftpServer.password)
      .port(ftpServer.port)
      .user(ftpServer.user)
      .build()
    try ftpClient.delete(props.inputFolder)
    finally ftpClient.close()

    val task = createTask()
    // input folder doesn't exist but no error is thrown.
    task.listInputFiles().size shouldBe 0
  }

  @Test
  def testListInput(): Unit = {
    val numberOfInputs = 3
    val ftpClient = FtpClient
      .builder()
      .hostname(ftpServer.hostname)
      .password(ftpServer.password)
      .port(ftpServer.port)
      .user(ftpServer.user)
      .build()
    try {
      val data = (0 to 100).map(_.toString)
      (0 until numberOfInputs).foreach(index =>
        ftpClient.attach(CommonUtil.path(props.inputFolder, index.toString), data))
    } finally ftpClient.close()

    val task = createTask()
    task.listInputFiles().size shouldBe 3
  }

  @Test
  def testToRow(): Unit = {
    val path = CommonUtil.path(props.inputFolder, methodName)
    val data = setupInputData(path)
    val task = createTask()
    task.cache = new FakeOffsetCache
    val rows = task.toRow(path)
    rows shouldBe data
  }

  @Test
  def testToRowIfAllCached(): Unit = {
    val path = CommonUtil.path(props.inputFolder, methodName)
    setupInputData(path)
    val task = createTask()
    task.cache = new OffsetCache {
      override def update(path: String, index: Int): Unit = {}

      override def predicate(path: String, index: Int): Boolean = false

      override def update(context: RowSourceContext, path: String): Unit = {}
    }
    val rows = task.toRow(path)
    rows.size shouldBe 0
  }

  @Test
  def testHandleCompletedFile(): Unit = {
    val path = CommonUtil.path(props.inputFolder, methodName)
    setupInputData(path)
    val task = createTask()
    task.handleCompletedFile(path)
    assertNumberOfFiles(0, 1, 0)
  }

  @Test
  def testHandleErrorFile(): Unit = {
    val path = CommonUtil.path(props.inputFolder, methodName)
    setupInputData(path)
    val task = createTask()
    task.handleErrorFile(path)
    assertNumberOfFiles(0, 0, 1)
  }

  @Test
  def testTransform(): Unit = {
    val path = CommonUtil.path(props.inputFolder, methodName)
    val data = setupInputData(path)
    val task = createTask()
    task.transform(data) shouldBe data.map {
      case (index, cells) => (index, Row.of(cells: _*))
    }
  }

  @Test
  def testTransformWithFullSchema(): Unit = {
    val path = CommonUtil.path(props.inputFolder, methodName)
    val data = setupInputData(path)
    val schema = data.head._2.map(_.name).zipWithIndex.map {
      case (name: String, index: Int) => Column.of(name, DataType.STRING, index)
    }
    val task = new FtpSourceTask()

    task._start(
      TaskConfig
        .builder()
        .name(methodName())
        .topic(methodName())
        .schema(schema.asJava)
        .options(props.toMap.asJava)
        .build())
    task.transform(data) shouldBe data.map {
      case (index, cells) => (index, Row.of(cells: _*))
    }
  }

  @Test
  def testTransformWithSingleColumn(): Unit = {
    val path = CommonUtil.path(props.inputFolder, methodName)
    val data = setupInputData(path)
    val schema = data.head._2
      .map(_.name)
      .zipWithIndex
      .map {
        case (name, index) => Column.of(name, DataType.STRING, index)
      }
      .head

    val task = new FtpSourceTask()

    task._start(
      TaskConfig.builder().name(methodName()).topic(methodName()).schema(schema).options(props.toMap.asJava).build())
    val transformedData = task.transform(data)
    transformedData.size shouldBe data.size
    transformedData.values.foreach(row => {
      row.size shouldBe 1
      // it should pass
      val cell = row.cell(schema.newName)
      cell.value.getClass shouldBe classOf[String]
    })
  }

  @Test
  def testRegex(): Unit = {
    val splits = "1,\"2,3,4\",5".split(FtpSourceTask.CSV_REGEX)
    splits.size shouldBe 3
    splits(0) shouldBe "1"
    splits(1) shouldBe "\"2,3,4\""
    splits(2) shouldBe "5"

    val splits2 = "1,3,5".split(FtpSourceTask.CSV_REGEX)
    splits2.size shouldBe 3
    splits2(0) shouldBe "1"
    splits2(1) shouldBe "3"
    splits2(2) shouldBe "5"
  }

  @After
  def tearDown(): Unit = Releasable.close(ftpServer)
}

class FakeOffsetCache extends OffsetCache {

  override def update(path: String, index: Int): Unit = {
    // DO NOTHING
  }

  override def predicate(path: String, index: Int): Boolean = true

  override def update(context: RowSourceContext, path: String): Unit = {
    // DO NOTHING
  }
}
