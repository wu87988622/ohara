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

import java.io.{BufferedReader, InputStreamReader}
import java.nio.charset.Charset
import java.nio.file.{Path, Paths}
import java.time.Duration
import java.util.concurrent.TimeUnit

import com.island.ohara.client.filesystem.FileSystem
import com.island.ohara.client.kafka.WorkerClient
import com.island.ohara.common.data.{Cell, DataType, Row, Serializer, _}
import com.island.ohara.common.setting.{ConnectorKey, TopicKey}
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.kafka.connector.json.ConnectorFormatter
import com.island.ohara.kafka.{BrokerClient, Consumer, Producer}
import com.island.ohara.testing.With3Brokers3Workers
import org.junit.{After, Before, BeforeClass, Test}
import org.scalatest.Matchers

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object TestFtpSink extends With3Brokers3Workers with Matchers {

  private val TOPIC = TopicKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))

  private val data = Row.of(Cell.of("a", "abc"), Cell.of("b", 123), Cell.of("c", true))

  private val dataCount = 1000

  @BeforeClass
  def init(): Unit = {
    this.setupData(TOPIC)
  }

  def setupData(topicKey: TopicKey): Unit = {
    val client = BrokerClient.of(testUtil.brokersConnProps)
    val topicName = topicKey.topicNameOnKafka
    try {
      if (client.exist(topicName)) client.deleteTopic(topicName)
      client.topicCreator().numberOfPartitions(1).numberOfReplications(1).compacted().topicName(topicName).create()
    } finally client.close()

    val producer = Producer
      .builder()
      .connectionProps(testUtil.brokersConnProps)
      .keySerializer(Serializer.ROW)
      .valueSerializer(Serializer.BYTES)
      .build()
    try {
      0 until dataCount foreach (_ => {
        producer.sender().key(data).topicName(topicName).send()
      })
      producer.flush()
    } finally producer.close()

    val consumer = Consumer
      .builder()
      .topicName(topicName)
      .offsetFromBegin()
      .connectionProps(testUtil.brokersConnProps)
      .keySerializer(Serializer.ROW)
      .valueSerializer(Serializer.BYTES)
      .build()
    try {
      val records = consumer.poll(java.time.Duration.ofSeconds(60), 1)
      val row = records.get(0).key().get
      row.size shouldBe data.size
      row.cell("a").value shouldBe "abc"
      row.cell("b").value shouldBe 123
      row.cell("c").value shouldBe true
    } finally consumer.close()
  }
}

class TestFtpSink extends With3Brokers3Workers with Matchers {

  private[this] val workerClient = WorkerClient(testUtil.workersConnProps)

  private[this] val TOPIC = TestFtpSink.TOPIC

  private[this] val schema: Seq[Column] = Seq(
    Column.builder().name("a").dataType(DataType.STRING).order(1).build(),
    Column.builder().name("b").dataType(DataType.INT).order(2).build(),
    Column.builder().name("c").dataType(DataType.BOOLEAN).order(3).build()
  )

  private[this] val data = TestFtpSink.data

  private[this] val props = FtpSinkProps(
    hostname = testUtil.ftpServer.hostname,
    port = testUtil.ftpServer.port,
    user = testUtil.ftpServer.user,
    password = testUtil.ftpServer.password,
    topicsDir = "/output",
    needHeader = false,
    encode = "UTF-8"
  )

  private[this] val fileSystem = FileSystem.ftpBuilder
    .hostname(testUtil.ftpServer.hostname)
    .port(testUtil.ftpServer.port)
    .user(testUtil.ftpServer.user)
    .password(testUtil.ftpServer.password)
    .build

  @Before
  def setup(): Unit = {
    fileSystem.reMkdirs(props.topicsDir)
    fileSystem.exists(props.topicsDir) shouldBe true
    fileSystem.listFileNames(props.topicsDir).asScala.size shouldBe 0
  }

  @After
  def tearDown(): Unit = Releasable.close(fileSystem)

  @Test
  def testReorder(): Unit = {
    val connectorKey = randomConnectorKey()
    val newSchema: Seq[Column] = Seq(
      Column.builder().name("a").dataType(DataType.STRING).order(3).build(),
      Column.builder().name("b").dataType(DataType.INT).order(2).build(),
      Column.builder().name("c").dataType(DataType.BOOLEAN).order(1).build()
    )
    createConnector(connectorKey, newSchema, props.toMap)

    try {
      FtpUtils.checkConnector(testUtil, connectorKey)
      val committedFolder = getCommittedFolder()
      checkCommittedFileSize(committedFolder, 1)
      val committedFile = listCommittedFiles(committedFolder).head
      val lines = readLines(committedFile)
      lines.length shouldBe 1000
      val items = lines.head.split(",")
      items.length shouldBe data.size
      items(0) shouldBe data.cell(2).value.toString
      items(1) shouldBe data.cell(1).value.toString
      items(2) shouldBe data.cell(0).value.toString
    } finally result(workerClient.delete(connectorKey))
  }

  @Test
  def testHeader(): Unit = {
    val connectorKey = randomConnectorKey()
    createConnector(connectorKey, schema, props.copy(needHeader = true).toMap)

    try {
      FtpUtils.checkConnector(testUtil, connectorKey)
      val committedFolder = getCommittedFolder()
      checkCommittedFileSize(committedFolder, 1)
      val committedFile = listCommittedFiles(committedFolder).head
      val lines = readLines(committedFile)
      lines.length shouldBe 1001
      lines.head shouldBe schema.sortBy(_.order).map(_.name).mkString(",")
      val items = lines(1).split(",")
      items.length shouldBe data.size
      items(0) shouldBe data.cell(0).value.toString
      items(1) shouldBe data.cell(1).value.toString
      items(2) shouldBe data.cell(2).value.toString
    } finally result(workerClient.delete(connectorKey))
  }

  @Test
  def testHeaderWithoutSchema(): Unit = {
    val connectorKey = randomConnectorKey()
    createConnector(connectorKey, props.copy(needHeader = true).toMap)

    try {
      FtpUtils.checkConnector(testUtil, connectorKey)
      val committedFolder = getCommittedFolder()
      checkCommittedFileSize(committedFolder, 1)
      val committedFile = listCommittedFiles(committedFolder).head
      val lines = readLines(committedFile)
      lines.length shouldBe 1001
      lines.head shouldBe data.cells().asScala.map(_.name).mkString(",")
      val items = lines(1).split(",")
      items.length shouldBe data.size
      items(0) shouldBe data.cell(0).value.toString
      items(1) shouldBe data.cell(1).value.toString
      items(2) shouldBe data.cell(2).value.toString
    } finally result(workerClient.delete(connectorKey))
  }

  @Test
  def testColumnRename(): Unit = {
    val connectorKey = randomConnectorKey()
    val newSchema = Seq(
      Column.builder().name("a").newName("aa").dataType(DataType.STRING).order(1).build(),
      Column.builder().name("b").newName("bb").dataType(DataType.INT).order(2).build(),
      Column.builder().name("c").newName("cc").dataType(DataType.BOOLEAN).order(3).build()
    )
    createConnector(connectorKey, newSchema, props.copy(needHeader = true).toMap)

    try {
      FtpUtils.checkConnector(testUtil, connectorKey)
      val committedFolder = getCommittedFolder()
      checkCommittedFileSize(committedFolder, 1)
      val committedFile = listCommittedFiles(committedFolder).head
      val lines = readLines(committedFile)
      lines.length shouldBe 1001
      lines.head shouldBe newSchema.sortBy(_.order).map(_.newName).mkString(",")
      val items = lines(1).split(",")
      items.length shouldBe data.size
      items(0) shouldBe data.cell(0).value.toString
      items(1) shouldBe data.cell(1).value.toString
      items(2) shouldBe data.cell(2).value.toString
    } finally result(workerClient.delete(connectorKey))
  }

  @Test
  def testNormalCase(): Unit = {
    val connectorKey = randomConnectorKey()
    createConnector(connectorKey, schema, props.toMap)

    try {
      FtpUtils.checkConnector(testUtil, connectorKey)
      val committedFolder = getCommittedFolder()
      checkCommittedFileSize(committedFolder, 1)
      val committedFile = listCommittedFiles(committedFolder).head
      val lines = readLines(committedFile)
      lines.length shouldBe 1000
      val items = lines.head.split(",")
      items.length shouldBe data.size
      items(0) shouldBe data.cell(0).value.toString
      items(1) shouldBe data.cell(1).value.toString
      items(2) shouldBe data.cell(2).value.toString
    } finally result(workerClient.delete(connectorKey))
  }

  @Test
  def testNormalCaseWithoutSchema(): Unit = {
    val connectorKey = randomConnectorKey()
    createConnector(connectorKey, props.toMap)

    try {
      FtpUtils.checkConnector(testUtil, connectorKey)
      val committedFolder = getCommittedFolder()
      checkCommittedFileSize(committedFolder, 1)
      val committedFile = listCommittedFiles(committedFolder).head
      val lines = readLines(committedFile)
      lines.length shouldBe 1000
      val items = lines.head.split(",")
      items.length shouldBe data.size
      items(0) shouldBe data.cell(0).value.toString
      items(1) shouldBe data.cell(1).value.toString
      items(2) shouldBe data.cell(2).value.toString
    } finally result(workerClient.delete(connectorKey))
  }

  @Test
  def testNormalCaseWithoutEncode(): Unit = {
    val connectorKey = randomConnectorKey()
    //will use default UTF-8
    val settings = props.toMap - FILE_ENCODE_CONFIG
    createConnector(connectorKey, schema, settings)

    try {
      FtpUtils.checkConnector(testUtil, connectorKey)
      val committedFolder = getCommittedFolder()
      checkCommittedFileSize(committedFolder, 1)
      val committedFile = listCommittedFiles(committedFolder).head
      val lines = readLines(committedFile)
      lines.length shouldBe 1000
      val items = lines.head.split(",")
      items.length shouldBe data.size
      items(0) shouldBe data.cell(0).value.toString
      items(1) shouldBe data.cell(1).value.toString
      items(2) shouldBe data.cell(2).value.toString
    } finally result(workerClient.delete(connectorKey))
  }

  @Test
  def testPartialColumns(): Unit = {
    val connectorKey = randomConnectorKey()
    // skip last column
    val newSchema = schema.slice(0, schema.length - 1)
    createConnector(connectorKey, newSchema, props.toMap)

    try {
      FtpUtils.checkConnector(testUtil, connectorKey)
      val committedFolder = getCommittedFolder()
      checkCommittedFileSize(committedFolder, 1)
      val committedFile = listCommittedFiles(committedFolder).head
      val lines = readLines(committedFile)
      lines.length shouldBe 1000
      val items = lines.head.split(",")
      items.length shouldBe data.size - 1
      items(0) shouldBe data.cell(0).value.toString
      items(1) shouldBe data.cell(1).value.toString
    } finally result(workerClient.delete(connectorKey))
  }

  @Test
  def testUnmatchedSchema(): Unit = {
    val connectorKey = randomConnectorKey()
    // the name can't be casted to int
    val newSchema = Seq(Column.builder().name("name").dataType(DataType.INT).order(1).build())
    createConnector(connectorKey, newSchema, props.toMap)

    try {
      FtpUtils.checkConnector(testUtil, connectorKey)
      TimeUnit.SECONDS.sleep(5)
      val folder = getCommittedFolder()
      fileSystem.exists(folder.toString) shouldBe false
    } finally result(workerClient.delete(connectorKey))
  }

  @Test
  def testAutoCreateOutput(): Unit = {
    val sink = new FtpSink
    val connectorKey = randomConnectorKey()
    val props = FtpSinkProps(
      topicsDir = "/output",
      needHeader = false,
      user = testUtil.ftpServer.user,
      password = testUtil.ftpServer.password,
      hostname = testUtil.ftpServer.hostname,
      port = testUtil.ftpServer.port,
      encode = "UTF-8"
    )
    sink.start(ConnectorFormatter.of().connectorKey(connectorKey).settings(props.toMap.asJava).raw())
    fileSystem.exists("/output") shouldBe true
  }

  @Test
  def testInvalidPort(): Unit = {
    val connectorKey = randomConnectorKey()
    Seq(-1, 0, 10000000).foreach { port =>
      an[IllegalArgumentException] should be thrownBy createConnector(connectorKey,
                                                                      schema,
                                                                      props.copy(port = port).toMap)
    }
  }

  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)

  private[this] def randomConnectorKey() = ConnectorKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))

  private[this] def createConnector(connectorKey: ConnectorKey, settings: Map[String, String]): Unit = result(
    workerClient
      .connectorCreator()
      .topicKey(TOPIC)
      .connectorClass(classOf[FtpSink])
      .numberOfTasks(1)
      .connectorKey(connectorKey)
      .settings(settings)
      .create())

  private[this] def createConnector(connectorKey: ConnectorKey,
                                    columns: Seq[Column],
                                    settings: Map[String, String]): Unit = result(
    workerClient
      .connectorCreator()
      .topicKey(TOPIC)
      .connectorClass(classOf[FtpSink])
      .numberOfTasks(1)
      .connectorKey(connectorKey)
      .columns(columns)
      .settings(settings)
      .create())

  private[this] def getCommittedFolder(): Path = Paths.get(props.topicsDir, TOPIC.topicNameOnKafka(), "partition0")

  private[this] def checkCommittedFileSize(dir: Path, expectedSize: Int): Unit = {
    CommonUtils.await(() => fileSystem.exists(dir.toString), Duration.ofSeconds(20))
    CommonUtils.await(() => listCommittedFiles(dir).size == expectedSize, Duration.ofSeconds(20))
  }

  private[this] def listCommittedFiles(folder: Path): Seq[Path] =
    fileSystem
      .listFileNames(folder.toString)
      .asScala
      .filter(!_.contains("_tmp"))
      .map(Paths.get(folder.toString, _))
      .toSeq

  private[this] def readLines(file: Path): Array[String] = {
    val reader = new BufferedReader(
      new InputStreamReader(fileSystem.open(file.toString), Charset.forName(props.encode)))
    try Iterator.continually(reader.readLine()).takeWhile(_ != null).toArray
    finally reader.close()
  }
}
