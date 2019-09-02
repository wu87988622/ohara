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
import com.island.ohara.common.setting.{ConnectorKey, TopicKey}
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.kafka.Consumer.Record
import com.island.ohara.kafka.connector.json.ConnectorFormatter
import com.island.ohara.kafka.{BrokerClient, Consumer}
import com.island.ohara.testing.With3Brokers3Workers
import org.junit.{After, Before, Test}
import org.scalatest.Matchers

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
class TestFtpSource extends With3Brokers3Workers with Matchers {

  private[this] val schema: Seq[Column] = Seq(
    Column.builder().name("name").dataType(DataType.STRING).order(1).build(),
    Column.builder().name("ranking").dataType(DataType.INT).order(2).build(),
    Column.builder().name("single").dataType(DataType.BOOLEAN).order(3).build()
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

  private[this] val ftpStorage = new FtpStorage(
    FtpClient
      .builder()
      .hostname(testUtil.ftpServer.hostname)
      .port(testUtil.ftpServer.port)
      .user(testUtil.ftpServer.user)
      .password(testUtil.ftpServer.password)
      .build())

  private[this] val props = FtpSourceProps(
    inputFolder = "/input",
    completedFolder = Some("/output"),
    errorFolder = "/error",
    user = testUtil.ftpServer.user,
    password = testUtil.ftpServer.password,
    hostname = testUtil.ftpServer.hostname,
    port = testUtil.ftpServer.port,
    encode = "UTF-8"
  )

  private[this] def setupInput(): Unit = {
    val writer = new BufferedWriter(
      new OutputStreamWriter(ftpStorage.create(CommonUtils.path(props.inputFolder, "abc"))))
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
      if (ftpStorage.exists(path)) {
        ftpStorage.delete(path, true)
      }
      ftpStorage.mkdirs(path)
      ftpStorage.list(path).asScala.size shouldBe 0
    }
    // cleanup all files in order to avoid corrupted files
    rebuild(props.inputFolder)
    rebuild(props.errorFolder)
    rebuild(props.completedFolder.get)
    setupInput()
    ftpStorage.list(props.inputFolder).asScala.isEmpty shouldBe false
    brokerClient.topicCreator().numberOfPartitions(1).numberOfReplications(1).topicName(methodName()).create()
    val topicInfo = brokerClient.topicDescription(methodName())
    topicInfo.numberOfPartitions() shouldBe 1
    topicInfo.numberOfReplications() shouldBe 1
  }

  private[this] def pollData(topicKey: TopicKey,
                             timeout: scala.concurrent.duration.Duration = 100 seconds,
                             size: Int = data.length): Seq[Record[Row, Array[Byte]]] = {
    val consumer = Consumer
      .builder[Row, Array[Byte]]()
      .topicName(topicKey.topicNameOnKafka)
      .offsetFromBegin()
      .connectionProps(testUtil.brokersConnProps)
      .keySerializer(Serializer.ROW)
      .valueSerializer(Serializer.BYTES)
      .build()
    try consumer.poll(java.time.Duration.ofNanos(timeout.toNanos), size).asScala
    finally consumer.close()
  }

  private[this] def checkFileCount(inputCount: Int, outputCount: Int, errorCount: Int): Unit = {
    CommonUtils.await(
      () => {
        ftpStorage.list(props.inputFolder).asScala.size == inputCount &&
        ftpStorage.list(props.completedFolder.get).asScala.size == outputCount &&
        ftpStorage.list(props.errorFolder).asScala.size == errorCount
      },
      Duration.ofSeconds(20)
    )
  }

  @Test
  def testDuplicateInput(): Unit = {
    val topicKey = TopicKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    val connectorKey = ConnectorKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    result(
      workerClient
        .connectorCreator()
        .topicKey(topicKey)
        .connectorClass(classOf[FtpSource])
        .numberOfTasks(1)
        .connectorKey(connectorKey)
        .columns(schema)
        .settings(props.toMap)
        .create())
    try {
      FtpUtils.checkConnector(testUtil, connectorKey)

      checkFileCount(0, 1, 0)
      var records = pollData(topicKey)
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
      records = pollData(topicKey, 10 second)
      records.size shouldBe data.length

    } finally result(workerClient.delete(connectorKey))
  }

  @Test
  def testColumnRename(): Unit = {
    val topicKey = TopicKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    val connectorKey = ConnectorKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    result(
      workerClient
        .connectorCreator()
        .topicKey(topicKey)
        .connectorClass(classOf[FtpSource])
        .numberOfTasks(1)
        .connectorKey(connectorKey)
        .columns(Seq(
          Column.builder().name("name").newName("newName").dataType(DataType.STRING).order(1).build(),
          Column.builder().name("ranking").newName("newRanking").dataType(DataType.INT).order(2).build(),
          Column.builder().name("single").newName("newSingle").dataType(DataType.BOOLEAN).order(3).build()
        ))
        .settings(props.toMap)
        .create())
    try {
      FtpUtils.checkConnector(testUtil, connectorKey)
      checkFileCount(0, 1, 0)

      val records = pollData(topicKey)
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

    } finally result(workerClient.delete(connectorKey))
  }

  @Test
  def testObjectType(): Unit = {
    val topicKey = TopicKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    val connectorKey = ConnectorKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    result(
      workerClient
        .connectorCreator()
        .topicKey(topicKey)
        .connectorClass(classOf[FtpSource])
        .numberOfTasks(1)
        .connectorKey(connectorKey)
        .columns(Seq(
          Column.builder().name("name").dataType(DataType.OBJECT).order(1).build(),
          Column.builder().name("ranking").dataType(DataType.INT).order(2).build(),
          Column.builder().name("single").dataType(DataType.BOOLEAN).order(3).build()
        ))
        .settings(props.toMap)
        .create())
    try {
      FtpUtils.checkConnector(testUtil, connectorKey)
      checkFileCount(0, 1, 0)

      val records = pollData(topicKey)
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

    } finally result(workerClient.delete(connectorKey))
  }

  @Test
  def testNormalCase(): Unit = {
    val topicKey = TopicKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    val connectorKey = ConnectorKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    result(
      workerClient
        .connectorCreator()
        .topicKey(topicKey)
        .connectorClass(classOf[FtpSource])
        .numberOfTasks(1)
        .connectorKey(connectorKey)
        .columns(schema)
        .settings(props.toMap)
        .create())
    try {
      FtpUtils.checkConnector(testUtil, connectorKey)
      checkFileCount(0, 1, 0)

      val records = pollData(topicKey)
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

    } finally result(workerClient.delete(connectorKey))

  }

  @Test
  def testNormalCaseWithoutSchema(): Unit = {
    val topicKey = TopicKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    val connectorKey = ConnectorKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    result(
      workerClient
        .connectorCreator()
        .topicKey(topicKey)
        .connectorClass(classOf[FtpSource])
        .numberOfTasks(1)
        .connectorKey(connectorKey)
        .settings(props.toMap)
        .create())
    try {
      FtpUtils.checkConnector(testUtil, connectorKey)
      checkFileCount(0, 1, 0)

      val records = pollData(topicKey)
      records.size shouldBe data.length
      val row0 = records.head.key.get
      row0.size shouldBe 3
      // NOTED: without columns all value are converted to string
      row0.cell(0) shouldBe Cell.of(rows.head.cell(0).name, rows.head.cell(0).value.toString)
      row0.cell(1) shouldBe Cell.of(rows.head.cell(1).name, rows.head.cell(1).value.toString)
      row0.cell(2) shouldBe Cell.of(rows.head.cell(2).name, rows.head.cell(2).value.toString)
      val row1 = records(1).key.get
      row1.size shouldBe 3
      row1.cell(0) shouldBe Cell.of(rows(1).cell(0).name, rows(1).cell(0).value.toString)
      row1.cell(1) shouldBe Cell.of(rows(1).cell(1).name, rows(1).cell(1).value.toString)
      row1.cell(2) shouldBe Cell.of(rows(1).cell(2).name, rows(1).cell(2).value.toString)

    } finally result(workerClient.delete(connectorKey))
  }

  @Test
  def testNormalCaseWithoutEncode(): Unit = {
    val topicKey = TopicKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    val connectorKey = ConnectorKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    result(
      workerClient
        .connectorCreator()
        .topicKey(topicKey)
        .connectorClass(classOf[FtpSource])
        .numberOfTasks(1)
        .connectorKey(connectorKey)
        .columns(schema)
        // will use default UTF-8
        .settings(props.toMap - FILE_ENCODE_CONFIG)
        .create())
    try {
      FtpUtils.checkConnector(testUtil, connectorKey)
      checkFileCount(0, 1, 0)

      val records = pollData(topicKey)
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

    } finally result(workerClient.delete(connectorKey))
  }

  @Test
  def testPartialColumns(): Unit = {
    val topicKey = TopicKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    val connectorKey = ConnectorKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    result(
      workerClient
        .connectorCreator()
        .topicKey(topicKey)
        .connectorClass(classOf[FtpSource])
        .numberOfTasks(1)
        .connectorKey(connectorKey)
        // skip last column
        .columns(schema.slice(0, schema.length - 1))
        .settings(props.toMap)
        .create())
    try {
      FtpUtils.checkConnector(testUtil, connectorKey)
      checkFileCount(0, 1, 0)

      val records = pollData(topicKey)
      records.size shouldBe data.length
      val row0 = records.head.key.get
      row0.size shouldBe 2
      row0.cell(0) shouldBe rows.head.cell(0)
      row0.cell(1) shouldBe rows.head.cell(1)
      val row1 = records(1).key.get
      row1.size shouldBe 2
      row1.cell(0) shouldBe rows(1).cell(0)
      row1.cell(1) shouldBe rows(1).cell(1)
    } finally result(workerClient.delete(connectorKey))
  }

  @Test
  def testUnmatchedSchema(): Unit = {
    val topicKey = TopicKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    val connectorKey = ConnectorKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    result(
      workerClient
        .connectorCreator()
        .topicKey(topicKey)
        .connectorClass(classOf[FtpSource])
        .numberOfTasks(1)
        .connectorKey(connectorKey)
        // the name can't be casted to int
        .columns(Seq(Column.builder().name("name").dataType(DataType.INT).order(1).build()))
        .settings(props.toMap)
        .create())
    try {
      FtpUtils.checkConnector(testUtil, connectorKey)
      checkFileCount(0, 0, 1)

      val records = pollData(topicKey, 10 second)
      records.size shouldBe 0

      // add a file to input again
      setupInput()
      checkFileCount(0, 0, 2)
    } finally result(workerClient.delete(connectorKey))
  }

  @Test
  def testInvalidInput(): Unit = {
    val topicKey = TopicKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    val connectorKey = ConnectorKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    result(
      workerClient
        .connectorCreator()
        .topicKey(topicKey)
        .connectorClass(classOf[FtpSource])
        .numberOfTasks(1)
        .connectorKey(connectorKey)
        .columns(schema)
        .settings(props.copy(inputFolder = "/abc").toMap)
        .create())
    FtpUtils.assertFailedConnector(testUtil, connectorKey)
  }

  @Test
  def testInvalidSchema(): Unit = {
    val topicKey = TopicKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    val connectorKey = ConnectorKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    result(
      workerClient
        .connectorCreator()
        .topicKey(topicKey)
        .connectorClass(classOf[FtpSource])
        .numberOfTasks(1)
        .connectorKey(connectorKey)
        .columns(Seq(
          // 0 is invalid
          Column.builder().name("name").dataType(DataType.STRING).order(0).build(),
          Column.builder().name("ranking").dataType(DataType.INT).order(2).build(),
          Column.builder().name("single").dataType(DataType.BOOLEAN).order(3).build()
        ))
        .settings(props.toMap)
        .create())
    FtpUtils.assertFailedConnector(testUtil, connectorKey)
  }

  @Test
  def inputFilesShouldBeRemovedIfCompletedFolderIsNotDefined(): Unit = {
    val topicKey = TopicKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    val connectorKey = ConnectorKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    result(
      workerClient
        .connectorCreator()
        .topicKey(topicKey)
        .connectorClass(classOf[FtpSource])
        .numberOfTasks(1)
        .connectorKey(connectorKey)
        .columns(schema)
        .settings(props.copy(completedFolder = None).toMap)
        .create())
    try {
      FtpUtils.checkConnector(testUtil, connectorKey)
      CommonUtils.await(
        () => {
          ftpStorage.list(props.inputFolder).asScala.size == 0
        },
        Duration.ofSeconds(20)
      )
      val records = pollData(topicKey)
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

    } finally result(workerClient.delete(connectorKey))
  }

  @Test
  def testAutoCreateOutput(): Unit = {
    val props = FtpSourceProps(
      inputFolder = "/input",
      completedFolder = Some("/output"),
      errorFolder = "/error",
      user = testUtil.ftpServer.user,
      password = testUtil.ftpServer.password,
      hostname = testUtil.ftpServer.hostname,
      port = testUtil.ftpServer.port,
      encode = "UTF-8"
    )

    ftpStorage.mkdirs(props.inputFolder)
    val source = new FtpSource
    val connectorKey = ConnectorKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    source.start(ConnectorFormatter.of().connectorKey(connectorKey).settings(props.toMap.asJava).raw())
    ftpStorage.exists(props.errorFolder) shouldBe true
    ftpStorage.exists(props.completedFolder.get) shouldBe true
  }

  @Test
  def testInvalidPort(): Unit = {
    val topicKey = TopicKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    val connectorKey = ConnectorKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    Seq(-1, 0, 10000000).foreach { port =>
      an[IllegalArgumentException] should be thrownBy result(
        workerClient
          .connectorCreator()
          .topicKey(topicKey)
          .connectorClass(classOf[FtpSource])
          .numberOfTasks(1)
          .connectorKey(connectorKey)
          .columns(schema)
          .settings(props.copy(port = port).toMap)
          .create())
    }
  }

  @After
  def tearDown(): Unit = {
    if (brokerClient.exist(methodName())) brokerClient.deleteTopic(methodName())
    Releasable.close(brokerClient)
    Releasable.close(ftpStorage)
  }
}
