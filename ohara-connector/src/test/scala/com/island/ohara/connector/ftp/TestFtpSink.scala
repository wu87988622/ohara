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
import java.time.Duration
import java.util.concurrent.TimeUnit

import com.island.ohara.client.ftp.FtpClient
import com.island.ohara.client.kafka.WorkerClient
import com.island.ohara.common.data.{Cell, DataType, Row, Serializer, _}
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.kafka.connector.json.ConnectorFormatter
import com.island.ohara.kafka.{BrokerClient, Consumer, Producer}
import com.island.ohara.testing.With3Brokers3Workers
import org.junit.{After, Before, BeforeClass, Test}
import org.scalatest.Matchers

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
object TestFtpSink extends With3Brokers3Workers with Matchers {

  private val TOPIC = "TestFtpSink"

  private val data = Row.of(Cell.of("a", "abc"), Cell.of("b", 123), Cell.of("c", true))

  @BeforeClass
  def init(): Unit = {
    this.setupData(TOPIC)
  }

  def setupData(topicName: String): Unit = {
    val client = BrokerClient.of(testUtil.brokersConnProps)
    try {
      if (client.exist(topicName)) client.deleteTopic(topicName)
      client.topicCreator().numberOfPartitions(1).numberOfReplications(1).compacted().topicName(topicName).create()
    } finally client.close()

    val producer = Producer
      .builder[Row, Array[Byte]]()
      .connectionProps(testUtil.brokersConnProps)
      .keySerializer(Serializer.ROW)
      .valueSerializer(Serializer.BYTES)
      .build()
    try producer.sender().key(data).topicName(topicName).send()
    finally producer.close()

    val consumer = Consumer
      .builder[Row, Array[Byte]]()
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
    outputFolder = "/output",
    needHeader = false,
    user = testUtil.ftpServer.user,
    password = testUtil.ftpServer.password,
    hostname = testUtil.ftpServer.hostname,
    port = testUtil.ftpServer.port,
    encode = "UTF-8"
  )

  private[this] val ftpClient = FtpClient
    .builder()
    .hostname(testUtil.ftpServer.hostname)
    .port(testUtil.ftpServer.port)
    .user(testUtil.ftpServer.user)
    .password(testUtil.ftpServer.password)
    .build()

  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)

  @Before
  def setup(): Unit = {
    if (ftpClient.exist(props.outputFolder)) {
      ftpClient
        .listFileNames(props.outputFolder)
        .map(com.island.ohara.common.util.CommonUtils.path(props.outputFolder, _))
        .foreach(ftpClient.delete)
      ftpClient.listFileNames(props.outputFolder).size shouldBe 0
      ftpClient.delete(props.outputFolder)
    }
    ftpClient.mkdir(props.outputFolder)

    ftpClient.listFileNames(props.outputFolder).size shouldBe 0
  }

  @Test
  def testReorder(): Unit = {
    val topicName = TOPIC
    val connectorName = methodName
    val newSchema: Seq[Column] = Seq(
      Column.builder().name("a").dataType(DataType.STRING).order(3).build(),
      Column.builder().name("b").dataType(DataType.INT).order(2).build(),
      Column.builder().name("c").dataType(DataType.BOOLEAN).order(1).build()
    )
    result(
      workerClient
        .connectorCreator()
        .topicName(topicName)
        .connectorClass(classOf[FtpSink])
        .numberOfTasks(1)
        .name(connectorName)
        .columns(newSchema)
        .settings(props.toMap)
        .create)

    try {
      FtpUtils.checkConnector(testUtil, connectorName)
      CommonUtils.await(() => ftpClient.listFileNames(props.outputFolder).size == 1, Duration.ofSeconds(20))
      val lines = ftpClient.readLines(
        com.island.ohara.common.util.CommonUtils
          .path(props.outputFolder, ftpClient.listFileNames(props.outputFolder).head))
      ftpClient.close()
      lines.length shouldBe 1
      val items = lines.head.split(",")
      items.length shouldBe data.size
      items(0) shouldBe data.cell(2).value.toString
      items(1) shouldBe data.cell(1).value.toString
      items(2) shouldBe data.cell(0).value.toString
    } finally result(workerClient.delete(connectorName))
  }

  @Test
  def testHeader(): Unit = {
    val topicName = TOPIC
    val connectorName = methodName
    result(
      workerClient
        .connectorCreator()
        .topicName(topicName)
        .connectorClass(classOf[FtpSink])
        .numberOfTasks(1)
        .name(connectorName)
        .columns(schema)
        .settings(props.copy(needHeader = true).toMap)
        .create)

    try {
      FtpUtils.checkConnector(testUtil, connectorName)
      CommonUtils.await(() => ftpClient.listFileNames(props.outputFolder).size == 1, Duration.ofSeconds(20))
      val lines = ftpClient.readLines(
        com.island.ohara.common.util.CommonUtils
          .path(props.outputFolder, ftpClient.listFileNames(props.outputFolder).head))
      ftpClient.close()
      lines.length shouldBe 2
      lines.head shouldBe schema.sortBy(_.order).map(_.name).mkString(",")
      val items = lines(1).split(",")
      items.length shouldBe data.size
      items(0) shouldBe data.cell(0).value.toString
      items(1) shouldBe data.cell(1).value.toString
      items(2) shouldBe data.cell(2).value.toString
    } finally result(workerClient.delete(connectorName))
  }

  @Test
  def testHeaderWithoutSchema(): Unit = {
    val topicName = TOPIC
    val connectorName = methodName
    result(
      workerClient
        .connectorCreator()
        .topicName(topicName)
        .connectorClass(classOf[FtpSink])
        .numberOfTasks(1)
        .name(connectorName)
        .settings(props.copy(needHeader = true).toMap)
        .create)

    try {
      FtpUtils.checkConnector(testUtil, connectorName)
      CommonUtils.await(() => ftpClient.listFileNames(props.outputFolder).size == 1, Duration.ofSeconds(20))
      val lines = ftpClient.readLines(
        com.island.ohara.common.util.CommonUtils
          .path(props.outputFolder, ftpClient.listFileNames(props.outputFolder).head))
      ftpClient.close()
      lines.length shouldBe 2
      lines.head shouldBe data.cells().asScala.map(_.name).mkString(",")
      val items = lines(1).split(",")
      items.length shouldBe data.size
      items(0) shouldBe data.cell(0).value.toString
      items(1) shouldBe data.cell(1).value.toString
      items(2) shouldBe data.cell(2).value.toString
    } finally result(workerClient.delete(connectorName))
  }

  @Test
  def testColumnRename(): Unit = {
    val topicName = TOPIC
    val connectorName = methodName
    val schema = Seq(
      Column.builder().name("a").newName("aa").dataType(DataType.STRING).order(1).build(),
      Column.builder().name("b").newName("bb").dataType(DataType.INT).order(2).build(),
      Column.builder().name("c").newName("cc").dataType(DataType.BOOLEAN).order(3).build()
    )
    result(
      workerClient
        .connectorCreator()
        .topicName(topicName)
        .connectorClass(classOf[FtpSink])
        .numberOfTasks(1)
        .name(connectorName)
        .columns(schema)
        .settings(props.copy(needHeader = true).toMap)
        .create)

    try {
      FtpUtils.checkConnector(testUtil, connectorName)
      CommonUtils.await(() => ftpClient.listFileNames(props.outputFolder).size == 1, Duration.ofSeconds(20))
      val lines = ftpClient.readLines(
        com.island.ohara.common.util.CommonUtils
          .path(props.outputFolder, ftpClient.listFileNames(props.outputFolder).head))
      ftpClient.close()
      lines.length shouldBe 2
      lines.head shouldBe schema.sortBy(_.order).map(_.newName).mkString(",")
      val items = lines(1).split(",")
      items.length shouldBe data.size
      items(0) shouldBe data.cell(0).value.toString
      items(1) shouldBe data.cell(1).value.toString
      items(2) shouldBe data.cell(2).value.toString
    } finally result(workerClient.delete(connectorName))
  }

  @Test
  def testNormalCase(): Unit = {
    val topicName = TOPIC
    val connectorName = methodName
    result(
      workerClient
        .connectorCreator()
        .topicName(topicName)
        .connectorClass(classOf[FtpSink])
        .numberOfTasks(1)
        .name(connectorName)
        .columns(schema)
        .settings(props.toMap)
        .create)

    try {
      FtpUtils.checkConnector(testUtil, connectorName)
      CommonUtils.await(() => ftpClient.listFileNames(props.outputFolder).size == 1, Duration.ofSeconds(20))
      val lines = ftpClient.readLines(
        com.island.ohara.common.util.CommonUtils
          .path(props.outputFolder, ftpClient.listFileNames(props.outputFolder).head))
      ftpClient.close()
      lines.length shouldBe 1
      val items = lines.head.split(",")
      items.length shouldBe data.size
      items(0) shouldBe data.cell(0).value.toString
      items(1) shouldBe data.cell(1).value.toString
      items(2) shouldBe data.cell(2).value.toString
    } finally result(workerClient.delete(connectorName))
  }

  @Test
  def testNormalCaseWithoutSchema(): Unit = {
    val topicName = TOPIC
    val connectorName = methodName
    result(
      workerClient
        .connectorCreator()
        .topicName(topicName)
        .connectorClass(classOf[FtpSink])
        .numberOfTasks(1)
        .name(connectorName)
        .settings(props.toMap)
        .create)

    try {
      FtpUtils.checkConnector(testUtil, connectorName)
      CommonUtils.await(() => ftpClient.listFileNames(props.outputFolder).size == 1, Duration.ofSeconds(20))
      val lines = ftpClient.readLines(
        com.island.ohara.common.util.CommonUtils
          .path(props.outputFolder, ftpClient.listFileNames(props.outputFolder).head))
      ftpClient.close()
      lines.length shouldBe 1
      val items = lines.head.split(",")
      items.length shouldBe data.size
      items(0) shouldBe data.cell(0).value.toString
      items(1) shouldBe data.cell(1).value.toString
      items(2) shouldBe data.cell(2).value.toString
    } finally result(workerClient.delete(connectorName))
  }

  @Test
  def testNormalCaseWithoutEncode(): Unit = {
    val topicName = TOPIC
    val connectorName = methodName
    result(
      workerClient
        .connectorCreator()
        .topicName(topicName)
        .connectorClass(classOf[FtpSink])
        .numberOfTasks(1)
        .name(connectorName)
        .columns(schema)
        //will use default UTF-8
        .settings(props.toMap - FTP_ENCODE)
        .create)

    try {
      FtpUtils.checkConnector(testUtil, connectorName)
      CommonUtils.await(() => ftpClient.listFileNames(props.outputFolder).size == 1, Duration.ofSeconds(20))
      val lines = ftpClient.readLines(
        com.island.ohara.common.util.CommonUtils
          .path(props.outputFolder, ftpClient.listFileNames(props.outputFolder).head))
      ftpClient.close()
      lines.length shouldBe 1
      val items = lines.head.split(",")
      items.length shouldBe data.size
      items(0) shouldBe data.cell(0).value.toString
      items(1) shouldBe data.cell(1).value.toString
      items(2) shouldBe data.cell(2).value.toString
    } finally result(workerClient.delete(connectorName))
  }

  @Test
  def testPartialColumns(): Unit = {
    val topicName = TOPIC
    val connectorName = methodName
    result(
      workerClient
        .connectorCreator()
        .topicName(topicName)
        .connectorClass(classOf[FtpSink])
        .numberOfTasks(1)
        .name(connectorName)
        // skip last column
        .columns(schema.slice(0, schema.length - 1))
        .settings(props.toMap)
        .create)

    try {
      FtpUtils.checkConnector(testUtil, connectorName)
      CommonUtils.await(() => ftpClient.listFileNames(props.outputFolder).size == 1, Duration.ofSeconds(20))
      val lines = ftpClient.readLines(
        com.island.ohara.common.util.CommonUtils
          .path(props.outputFolder, ftpClient.listFileNames(props.outputFolder).head))
      ftpClient.close()
      lines.length shouldBe 1
      val items = lines.head.split(",")
      items.length shouldBe data.size - 1
      items(0) shouldBe data.cell(0).value.toString
      items(1) shouldBe data.cell(1).value.toString
    } finally result(workerClient.delete(connectorName))
  }

  @Test
  def testUnmatchedSchema(): Unit = {
    val topicName = TOPIC
    val connectorName = methodName
    result(
      workerClient
        .connectorCreator()
        .topicName(topicName)
        .connectorClass(classOf[FtpSink])
        .numberOfTasks(1)
        .name(connectorName)
        // the name can't be casted to int
        .columns(Seq(Column.builder().name("name").dataType(DataType.INT).order(1).build()))
        .settings(props.toMap)
        .create)

    try {
      FtpUtils.checkConnector(testUtil, connectorName)
      TimeUnit.SECONDS.sleep(5)
      ftpClient.listFileNames(props.outputFolder).size shouldBe 0
      ftpClient.close()
    } finally result(workerClient.delete(connectorName))
  }

  @Test
  def testAutoCreateOutput(): Unit = {
    val props = FtpSinkProps(
      outputFolder = "/output",
      needHeader = false,
      user = testUtil.ftpServer.user,
      password = testUtil.ftpServer.password,
      hostname = testUtil.ftpServer.hostname,
      port = testUtil.ftpServer.port,
      encode = "UTF-8"
    )

    val sink = new FtpSink
    sink.start(ConnectorFormatter.of().name("aa").settings(props.toMap.asJava).raw())

    ftpClient.exist("/output") shouldBe true
  }

  @Test
  def testInvalidPort(): Unit = {
    Seq(-1, 0, 10000000).foreach { port =>
      an[IllegalArgumentException] should be thrownBy result(
        workerClient
          .connectorCreator()
          .topicName(methodName())
          .connectorClass(classOf[FtpSink])
          .numberOfTasks(1)
          .name(CommonUtils.randomString(10))
          .columns(schema)
          .settings(props.copy(port = port).toMap)
          .create)
    }
  }

  @After
  def tearDown(): Unit = Releasable.close(ftpClient)
}
