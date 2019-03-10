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
import java.io.{BufferedWriter, OutputStreamWriter}
import java.time.Duration

import com.island.ohara.client.ftp.FtpClient
import com.island.ohara.client.kafka.WorkerClient
import com.island.ohara.common.data.{Cell, DataType, Row, Serializer, _}
import com.island.ohara.common.util.{CommonUtil, Releasable}
import com.island.ohara.kafka.Consumer.Record
import com.island.ohara.kafka.{BrokerClient, Consumer}
import com.island.ohara.testing.With3Brokers3Workers
import org.junit.{After, Before, Test}
import org.scalatest.Matchers

import scala.collection.JavaConverters._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
class TestFtpSource extends With3Brokers3Workers with Matchers {

  private[this] val schema: Seq[Column] = Seq(
    Column.of("name", DataType.STRING, 1),
    Column.of("ranking", DataType.INT, 2),
    Column.of("single", DataType.BOOLEAN, 3)
  )
  private[this] val rows: Seq[Row] = Seq(
    Row.of(Cell.of("name", "chia"), Cell.of("ranking", 1), Cell.of("single", false)),
    Row.of(Cell.of("name", "jack"), Cell.of("ranking", 99), Cell.of("single", true))
  )
  private[this] val header: String = rows.head.cells().asScala.map(_.name).mkString(",")
  private[this] val data: Seq[String] = rows.map(row => {
    row.cells().asScala.map(_.value.toString).mkString(",")
  })

  private[this] val brokerClient = BrokerClient.of(testUtil.brokersConnProps())
  private[this] val workerClient = WorkerClient(testUtil.workersConnProps)

  private[this] val ftpClient = FtpClient
    .builder()
    .hostname(testUtil.ftpServer.hostname)
    .port(testUtil.ftpServer.port)
    .user(testUtil.ftpServer.user)
    .password(testUtil.ftpServer.password)
    .build()

  private[this] val props = FtpSourceProps(
    inputFolder = "/input",
    completedFolder = "/output",
    errorFolder = "/error",
    user = testUtil.ftpServer.user,
    password = testUtil.ftpServer.password,
    hostname = testUtil.ftpServer.hostname,
    port = testUtil.ftpServer.port,
    encode = Some("UTF-8")
  )

  private[this] def setupInput(): Unit = {
    val writer = new BufferedWriter(
      new OutputStreamWriter(ftpClient.create(com.island.ohara.common.util.CommonUtil.path(props.inputFolder, "abc"))))
    try {
      writer.append(header)
      writer.newLine()
      data.foreach(line => {
        writer.append(line)
        writer.newLine()
      })
    } finally writer.close()
  }

  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)

  @Before
  def setup(): Unit = {
    def rebuild(path: String): Unit = {
      if (ftpClient.exist(path)) {
        ftpClient
          .listFileNames(path)
          .map(com.island.ohara.common.util.CommonUtil.path(path, _))
          .foreach(ftpClient.delete)
        ftpClient.listFileNames(path).size shouldBe 0
        ftpClient.delete(path)
      }
      ftpClient.mkdir(path)
    }
    // cleanup all files in order to avoid corrupted files
    rebuild(props.inputFolder)
    rebuild(props.errorFolder)
    rebuild(props.completedFolder.get)
    setupInput()
    ftpClient.listFileNames(props.inputFolder).isEmpty shouldBe false
    brokerClient.topicCreator().numberOfPartitions(1).numberOfReplications(1).create(methodName())
    val topicInfo = brokerClient.topicDescription(methodName())
    topicInfo.numberOfPartitions() shouldBe 1
    topicInfo.numberOfReplications() shouldBe 1
  }

  private[this] def pollData(topicName: String,
                             timeout: scala.concurrent.duration.Duration = 100 seconds,
                             size: Int = data.length): Seq[Record[Row, Array[Byte]]] = {
    val consumer = Consumer
      .builder[Row, Array[Byte]]()
      .topicName(methodName)
      .offsetFromBegin()
      .connectionProps(testUtil.brokersConnProps)
      .keySerializer(Serializer.ROW)
      .valueSerializer(Serializer.BYTES)
      .build()
    try consumer.poll(java.time.Duration.ofNanos(timeout.toNanos), size).asScala
    finally consumer.close()
  }

  private[this] def checkFileCount(inputCount: Int, outputCount: Int, errorCount: Int): Unit = {
    CommonUtil.await(
      () => {
        ftpClient.listFileNames(props.inputFolder).size == inputCount &&
        ftpClient.listFileNames(props.completedFolder.get).size == outputCount &&
        ftpClient.listFileNames(props.errorFolder).size == errorCount
      },
      Duration.ofSeconds(30)
    )
  }

  @Test
  def testDuplicateInput(): Unit = {
    val topicName = methodName
    val connectorName = methodName
    result(
      workerClient
        .connectorCreator()
        .topic(topicName)
        .connectorClass(classOf[FtpSource])
        .numberOfTasks(1)
        .disableConverter()
        .name(connectorName)
        .schema(schema)
        .configs(props.toMap)
        .create())
    try {
      FtpUtil.checkConnector(testUtil, connectorName)

      checkFileCount(0, 1, 0)
      var records = pollData(topicName)
      records.size shouldBe data.length
      val row0 = records.head.key.get
      row0.size shouldBe 3
      row0.cell(0) shouldBe rows.head.cell(0)
      row0.cell(1) shouldBe rows.head.cell(1)
      row0.cell(2) shouldBe rows.head.cell(2)
      val row1 = records(1).key.get
      row1.size shouldBe 3
      row1.cell(0) shouldBe rows(1).cell(0)
      row1.cell(1) shouldBe rows(1).cell(1)
      row1.cell(2) shouldBe rows(1).cell(2)

      // put a duplicate file
      setupInput()
      checkFileCount(0, 2, 0)
      records = pollData(topicName, 10 second)
      records.size shouldBe data.length

    } finally result(workerClient.delete(connectorName))
  }

  @Test
  def testColumnRename(): Unit = {
    val topicName = methodName
    val connectorName = methodName
    result(
      workerClient
        .connectorCreator()
        .topic(topicName)
        .connectorClass(classOf[FtpSource])
        .numberOfTasks(1)
        .disableConverter()
        .name(connectorName)
        .schema(
          Seq(
            Column.of("name", "newName", DataType.STRING, 1),
            Column.of("ranking", "newRanking", DataType.INT, 2),
            Column.of("single", "newSingle", DataType.BOOLEAN, 3)
          ))
        .configs(props.toMap)
        .create())
    try {
      FtpUtil.checkConnector(testUtil, connectorName)
      checkFileCount(0, 1, 0)

      val records = pollData(topicName)
      records.size shouldBe data.length
      val row0 = records.head.key.get
      row0.size shouldBe 3
      row0.cell(0).name shouldBe "newName"
      row0.cell(0).value shouldBe rows.head.cell(0).value
      row0.cell(1).name shouldBe "newRanking"
      row0.cell(1).value shouldBe rows.head.cell(1).value
      row0.cell(2).name shouldBe "newSingle"
      row0.cell(2).value shouldBe rows.head.cell(2).value
      val row1 = records(1).key.get
      row1.size shouldBe 3
      row0.cell(0).name shouldBe "newName"
      row1.cell(0).value shouldBe rows(1).cell(0).value
      row0.cell(1).name shouldBe "newRanking"
      row1.cell(1).value shouldBe rows(1).cell(1).value
      row0.cell(2).name shouldBe "newSingle"
      row1.cell(2).value shouldBe rows(1).cell(2).value

    } finally result(workerClient.delete(connectorName))
  }

  @Test
  def testObjectType(): Unit = {
    val topicName = methodName
    val connectorName = methodName
    result(
      workerClient
        .connectorCreator()
        .topic(topicName)
        .connectorClass(classOf[FtpSource])
        .numberOfTasks(1)
        .disableConverter()
        .name(connectorName)
        .schema(
          Seq(
            Column.of("name", DataType.OBJECT, 1),
            Column.of("ranking", DataType.INT, 2),
            Column.of("single", DataType.BOOLEAN, 3)
          ))
        .configs(props.toMap)
        .create())
    try {
      FtpUtil.checkConnector(testUtil, connectorName)
      checkFileCount(0, 1, 0)

      val records = pollData(topicName)
      records.size shouldBe data.length
      val row0 = records.head.key.get
      row0.size shouldBe 3
      row0.cell(0) shouldBe rows.head.cell(0)
      row0.cell(1) shouldBe rows.head.cell(1)
      row0.cell(2) shouldBe rows.head.cell(2)
      val row1 = records(1).key.get
      row1.size shouldBe 3
      row1.cell(0) shouldBe rows(1).cell(0)
      row1.cell(1) shouldBe rows(1).cell(1)
      row1.cell(2) shouldBe rows(1).cell(2)

    } finally result(workerClient.delete(connectorName))
  }

  @Test
  def testNormalCase(): Unit = {
    val topicName = methodName
    val connectorName = methodName
    result(
      workerClient
        .connectorCreator()
        .topic(topicName)
        .connectorClass(classOf[FtpSource])
        .numberOfTasks(1)
        .disableConverter()
        .name(connectorName)
        .schema(schema)
        .configs(props.toMap)
        .create())
    try {
      FtpUtil.checkConnector(testUtil, connectorName)
      checkFileCount(0, 1, 0)

      val records = pollData(topicName)
      records.size shouldBe data.length
      val row0 = records.head.key.get
      row0.size shouldBe 3
      row0.cell(0) shouldBe rows.head.cell(0)
      row0.cell(1) shouldBe rows.head.cell(1)
      row0.cell(2) shouldBe rows.head.cell(2)
      val row1 = records(1).key.get
      row1.size shouldBe 3
      row1.cell(0) shouldBe rows(1).cell(0)
      row1.cell(1) shouldBe rows(1).cell(1)
      row1.cell(2) shouldBe rows(1).cell(2)

    } finally result(workerClient.delete(connectorName))

  }

  @Test
  def testNormalCaseWithoutSchema(): Unit = {
    val topicName = methodName
    val connectorName = methodName
    result(
      workerClient
        .connectorCreator()
        .topic(topicName)
        .connectorClass(classOf[FtpSource])
        .numberOfTasks(1)
        .disableConverter()
        .name(connectorName)
        .configs(props.toMap)
        .create())
    try {
      FtpUtil.checkConnector(testUtil, connectorName)
      checkFileCount(0, 1, 0)

      val records = pollData(topicName)
      records.size shouldBe data.length
      val row0 = records.head.key.get
      row0.size shouldBe 3
      // NOTED: without schema all value are converted to string
      row0.cell(0) shouldBe Cell.of(rows.head.cell(0).name, rows.head.cell(0).value.toString)
      row0.cell(1) shouldBe Cell.of(rows.head.cell(1).name, rows.head.cell(1).value.toString)
      row0.cell(2) shouldBe Cell.of(rows.head.cell(2).name, rows.head.cell(2).value.toString)
      val row1 = records(1).key.get
      row1.size shouldBe 3
      row1.cell(0) shouldBe Cell.of(rows(1).cell(0).name, rows(1).cell(0).value.toString)
      row1.cell(1) shouldBe Cell.of(rows(1).cell(1).name, rows(1).cell(1).value.toString)
      row1.cell(2) shouldBe Cell.of(rows(1).cell(2).name, rows(1).cell(2).value.toString)

    } finally result(workerClient.delete(connectorName))
  }

  @Test
  def testNormalCaseWithNullEncode(): Unit = {
    val topicName = methodName
    val connectorName = methodName
    result(
      workerClient
        .connectorCreator()
        .topic(topicName)
        .connectorClass(classOf[FtpSource])
        .numberOfTasks(1)
        .disableConverter()
        .name(connectorName)
        .schema(schema)
        // will use default UTF-8
        .configs(props.copy(encode = None).toMap)
        .create())
    try {
      FtpUtil.checkConnector(testUtil, connectorName)
      checkFileCount(0, 1, 0)

      val records = pollData(topicName)
      records.size shouldBe data.length
      val row0 = records.head.key.get
      row0.size shouldBe 3
      row0.cell(0) shouldBe rows.head.cell(0)
      row0.cell(1) shouldBe rows.head.cell(1)
      row0.cell(2) shouldBe rows.head.cell(2)
      val row1 = records(1).key.get
      row1.size shouldBe 3
      row1.cell(0) shouldBe rows(1).cell(0)
      row1.cell(1) shouldBe rows(1).cell(1)
      row1.cell(2) shouldBe rows(1).cell(2)

    } finally result(workerClient.delete(connectorName))
  }

  @Test
  def testNormalCaseWithEmptyEncode(): Unit = {
    val topicName = methodName
    val connectorName = methodName
    result(
      workerClient
        .connectorCreator()
        .topic(topicName)
        .connectorClass(classOf[FtpSource])
        .numberOfTasks(1)
        .disableConverter()
        .name(connectorName)
        .schema(schema)
        // will use default UTF-8
        .configs(props.copy(encode = Some("")).toMap)
        .create())
    try {
      FtpUtil.checkConnector(testUtil, connectorName)
      checkFileCount(0, 1, 0)

      val records = pollData(topicName)
      records.size shouldBe data.length
      val row0 = records.head.key.get
      row0.size shouldBe 3
      row0.cell(0) shouldBe rows.head.cell(0)
      row0.cell(1) shouldBe rows.head.cell(1)
      row0.cell(2) shouldBe rows.head.cell(2)
      val row1 = records(1).key.get
      row1.size shouldBe 3
      row1.cell(0) shouldBe rows(1).cell(0)
      row1.cell(1) shouldBe rows(1).cell(1)
      row1.cell(2) shouldBe rows(1).cell(2)

    } finally result(workerClient.delete(connectorName))
  }

  @Test
  def testPartialColumns(): Unit = {
    val topicName = methodName
    val connectorName = methodName
    result(
      workerClient
        .connectorCreator()
        .topic(topicName)
        .connectorClass(classOf[FtpSource])
        .numberOfTasks(1)
        .disableConverter()
        .name(connectorName)
        // skip last column
        .schema(schema.slice(0, schema.length - 1))
        .configs(props.toMap)
        .create())
    try {
      FtpUtil.checkConnector(testUtil, connectorName)
      checkFileCount(0, 1, 0)

      val records = pollData(topicName)
      records.size shouldBe data.length
      val row0 = records.head.key.get
      row0.size shouldBe 2
      row0.cell(0) shouldBe rows.head.cell(0)
      row0.cell(1) shouldBe rows.head.cell(1)
      val row1 = records(1).key.get
      row1.size shouldBe 2
      row1.cell(0) shouldBe rows(1).cell(0)
      row1.cell(1) shouldBe rows(1).cell(1)
    } finally result(workerClient.delete(connectorName))
  }

  @Test
  def testUnmatchedSchema(): Unit = {
    val topicName = methodName
    val connectorName = methodName
    result(
      workerClient
        .connectorCreator()
        .topic(topicName)
        .connectorClass(classOf[FtpSource])
        .numberOfTasks(1)
        .disableConverter()
        .name(connectorName)
        // the name can't be casted to int
        .schema(Seq(Column.of("name", DataType.INT, 1)))
        .configs(props.toMap)
        .create())
    try {
      FtpUtil.checkConnector(testUtil, connectorName)
      checkFileCount(0, 0, 1)

      val records = pollData(topicName, 10 second)
      records.size shouldBe 0

      // add a file to input again
      setupInput()
      checkFileCount(0, 0, 2)
    } finally result(workerClient.delete(connectorName))
  }

  @Test
  def testInvalidInput(): Unit = {
    val topicName = methodName
    val connectorName = methodName
    result(
      workerClient
        .connectorCreator()
        .topic(topicName)
        .connectorClass(classOf[FtpSource])
        .numberOfTasks(1)
        .disableConverter()
        .name(connectorName)
        .schema(schema)
        .configs(props.copy(inputFolder = "/abc").toMap)
        .create())
    FtpUtil.assertFailedConnector(testUtil, connectorName)
  }

  @Test
  def testInvalidSchema(): Unit = {
    val topicName = methodName
    val connectorName = methodName
    result(
      workerClient
        .connectorCreator()
        .topic(topicName)
        .connectorClass(classOf[FtpSource])
        .numberOfTasks(1)
        .disableConverter()
        .name(connectorName)
        .schema(
          Seq(
            // 0 is invalid
            Column.of("name", DataType.STRING, 0),
            Column.of("ranking", DataType.INT, 2),
            Column.of("single", DataType.BOOLEAN, 3)
          ))
        .configs(props.toMap)
        .create())
    FtpUtil.assertFailedConnector(testUtil, connectorName)
  }

  @Test
  def inputFilesShouldBeRemovedIfCompletedFolderIsNotDefined(): Unit = {
    val topicName = methodName
    val connectorName = methodName
    result(
      workerClient
        .connectorCreator()
        .topic(topicName)
        .connectorClass(classOf[FtpSource])
        .numberOfTasks(1)
        .disableConverter()
        .name(connectorName)
        .schema(schema)
        .configs(props.copy(completedFolder = None).toMap)
        .create())
    try {
      FtpUtil.checkConnector(testUtil, connectorName)
      checkFileCount(0, 0, 0)

      val records = pollData(topicName)
      records.size shouldBe data.length
      val row0 = records.head.key.get
      row0.size shouldBe 3
      row0.cell(0) shouldBe rows.head.cell(0)
      row0.cell(1) shouldBe rows.head.cell(1)
      row0.cell(2) shouldBe rows.head.cell(2)
      val row1 = records(1).key.get
      row1.size shouldBe 3
      row1.cell(0) shouldBe rows(1).cell(0)
      row1.cell(1) shouldBe rows(1).cell(1)
      row1.cell(2) shouldBe rows(1).cell(2)

    } finally result(workerClient.delete(connectorName))

  }

  @After
  def tearDown(): Unit = {
    if (brokerClient.exist(methodName())) brokerClient.deleteTopic(methodName())
    Releasable.close(brokerClient)
    Releasable.close(ftpClient)
  }
}
