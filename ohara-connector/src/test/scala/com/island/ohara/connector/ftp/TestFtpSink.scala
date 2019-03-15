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
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.kafka.{BrokerClient, Consumer, Producer}
import com.island.ohara.testing.With3Brokers3Workers
import org.junit.{Before, BeforeClass, Test}
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
      client.topicCreator().numberOfPartitions(1).numberOfReplications(1).compacted().create(topicName)
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
    Column.newBuilder().name("a").dataType(DataType.STRING).order(1).build(),
    Column.newBuilder().name("b").dataType(DataType.INT).order(2).build(),
    Column.newBuilder().name("c").dataType(DataType.BOOLEAN).order(3).build()
  )

  private[this] val data = TestFtpSink.data

  private[this] val props = FtpSinkProps(
    output = "/output",
    needHeader = false,
    user = testUtil.ftpServer.user,
    password = testUtil.ftpServer.password,
    hostname = testUtil.ftpServer.hostname,
    port = testUtil.ftpServer.port,
    encode = Some("UTF-8")
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
    if (ftpClient.exist(props.output)) {
      ftpClient
        .listFileNames(props.output)
        .map(com.island.ohara.common.util.CommonUtils.path(props.output, _))
        .foreach(ftpClient.delete)
      ftpClient.listFileNames(props.output).size shouldBe 0
      ftpClient.delete(props.output)
    }
    ftpClient.mkdir(props.output)

    ftpClient.listFileNames(props.output).size shouldBe 0
  }

  @Test
  def testReorder(): Unit = {
    val topicName = TOPIC
    val connectorName = methodName
    val newSchema: Seq[Column] = Seq(
      Column.newBuilder().name("a").dataType(DataType.STRING).order(3).build(),
      Column.newBuilder().name("b").dataType(DataType.INT).order(2).build(),
      Column.newBuilder().name("c").dataType(DataType.BOOLEAN).order(1).build()
    )
    result(
      workerClient
        .connectorCreator()
        .topicName(topicName)
        .connectorClass(classOf[FtpSink])
        .numberOfTasks(1)
        .disableConverter()
        .name(connectorName)
        .columns(newSchema)
        .configs(props.toMap)
        .create)

    try {
      FtpUtils.checkConnector(testUtil, connectorName)
      CommonUtils.await(() => ftpClient.listFileNames(props.output).size == 1, Duration.ofSeconds(20))
      val lines = ftpClient.readLines(
        com.island.ohara.common.util.CommonUtils.path(props.output, ftpClient.listFileNames(props.output).head))
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
        .disableConverter()
        .name(connectorName)
        .columns(schema)
        .configs(props.copy(needHeader = true).toMap)
        .create)

    try {
      FtpUtils.checkConnector(testUtil, connectorName)
      CommonUtils.await(() => ftpClient.listFileNames(props.output).size == 1, Duration.ofSeconds(20))
      val lines = ftpClient.readLines(
        com.island.ohara.common.util.CommonUtils.path(props.output, ftpClient.listFileNames(props.output).head))
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
        .disableConverter()
        .name(connectorName)
        .configs(props.copy(needHeader = true).toMap)
        .create)

    try {
      FtpUtils.checkConnector(testUtil, connectorName)
      CommonUtils.await(() => ftpClient.listFileNames(props.output).size == 1, Duration.ofSeconds(20))
      val lines = ftpClient.readLines(
        com.island.ohara.common.util.CommonUtils.path(props.output, ftpClient.listFileNames(props.output).head))
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
      Column.newBuilder().name("a").newName("aa").dataType(DataType.STRING).order(1).build(),
      Column.newBuilder().name("b").newName("bb").dataType(DataType.INT).order(2).build(),
      Column.newBuilder().name("c").newName("cc").dataType(DataType.BOOLEAN).order(3).build()
    )
    result(
      workerClient
        .connectorCreator()
        .topicName(topicName)
        .connectorClass(classOf[FtpSink])
        .numberOfTasks(1)
        .disableConverter()
        .name(connectorName)
        .columns(schema)
        .configs(props.copy(needHeader = true).toMap)
        .create)

    try {
      FtpUtils.checkConnector(testUtil, connectorName)
      CommonUtils.await(() => ftpClient.listFileNames(props.output).size == 1, Duration.ofSeconds(20))
      val lines = ftpClient.readLines(
        com.island.ohara.common.util.CommonUtils.path(props.output, ftpClient.listFileNames(props.output).head))
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
        .disableConverter()
        .name(connectorName)
        .columns(schema)
        .configs(props.toMap)
        .create)

    try {
      FtpUtils.checkConnector(testUtil, connectorName)
      CommonUtils.await(() => ftpClient.listFileNames(props.output).size == 1, Duration.ofSeconds(20))
      val lines = ftpClient.readLines(
        com.island.ohara.common.util.CommonUtils.path(props.output, ftpClient.listFileNames(props.output).head))
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
        .disableConverter()
        .name(connectorName)
        .configs(props.toMap)
        .create)

    try {
      FtpUtils.checkConnector(testUtil, connectorName)
      CommonUtils.await(() => ftpClient.listFileNames(props.output).size == 1, Duration.ofSeconds(20))
      val lines = ftpClient.readLines(
        com.island.ohara.common.util.CommonUtils.path(props.output, ftpClient.listFileNames(props.output).head))
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
  def testNormalCaseWithNullEncode(): Unit = {
    val topicName = TOPIC
    val connectorName = methodName
    result(
      workerClient
        .connectorCreator()
        .topicName(topicName)
        .connectorClass(classOf[FtpSink])
        .numberOfTasks(1)
        .disableConverter()
        .name(connectorName)
        .columns(schema)
        //will use default UTF-8
        .configs(props.copy(encode = None).toMap)
        .create)

    try {
      FtpUtils.checkConnector(testUtil, connectorName)
      CommonUtils.await(() => ftpClient.listFileNames(props.output).size == 1, Duration.ofSeconds(20))
      val lines = ftpClient.readLines(
        com.island.ohara.common.util.CommonUtils.path(props.output, ftpClient.listFileNames(props.output).head))
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
  def testNormalCaseWithEmptyEncode(): Unit = {
    val topicName = TOPIC
    val connectorName = methodName
    result(
      workerClient
        .connectorCreator()
        .topicName(topicName)
        .connectorClass(classOf[FtpSink])
        .numberOfTasks(1)
        .disableConverter()
        .name(connectorName)
        .columns(schema)
        //will use default UTF-8
        .configs(props.copy(encode = Some("")).toMap)
        .create)

    try {
      FtpUtils.checkConnector(testUtil, connectorName)
      CommonUtils.await(() => ftpClient.listFileNames(props.output).size == 1, Duration.ofSeconds(20))
      val lines = ftpClient.readLines(
        com.island.ohara.common.util.CommonUtils.path(props.output, ftpClient.listFileNames(props.output).head))
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
        .disableConverter()
        .name(connectorName)
        // skip last column
        .columns(schema.slice(0, schema.length - 1))
        .configs(props.toMap)
        .create)

    try {
      FtpUtils.checkConnector(testUtil, connectorName)
      CommonUtils.await(() => ftpClient.listFileNames(props.output).size == 1, Duration.ofSeconds(20))
      val lines = ftpClient.readLines(
        com.island.ohara.common.util.CommonUtils.path(props.output, ftpClient.listFileNames(props.output).head))
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
        .disableConverter()
        .name(connectorName)
        // the name can't be casted to int
        .columns(Seq(Column.newBuilder().name("name").dataType(DataType.INT).order(1).build()))
        .configs(props.toMap)
        .create)

    try {
      FtpUtils.checkConnector(testUtil, connectorName)
      TimeUnit.SECONDS.sleep(5)
      ftpClient.listFileNames(props.output).size shouldBe 0
      ftpClient.close()
    } finally result(workerClient.delete(connectorName))
  }
}
