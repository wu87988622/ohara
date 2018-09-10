package com.island.ohara.connector.ftp
import java.util.concurrent.TimeUnit

import com.island.ohara.client.ConfiguratorJson.Column
import com.island.ohara.data.{Cell, Row}
import com.island.ohara.integration.{OharaTestUtil, With3Brokers3Workers}
import com.island.ohara.io.{ByteUtil, CloseOnce, IoUtil}
import com.island.ohara.kafka.{Consumer, KafkaClient, Producer}
import com.island.ohara.serialization.DataType
import org.junit.{Before, Test}
import org.scalatest.Matchers

import scala.concurrent.duration._
class TestFtpSink extends With3Brokers3Workers with Matchers {
  private[this] val schema: Seq[Column] = Seq(
    Column("a", DataType.STRING, 1),
    Column("b", DataType.INT, 2),
    Column("c", DataType.BOOLEAN, 3)
  )

  private[this] val data = Row(Cell("a", "abc"), Cell("b", 123), Cell("c", true))

  private[this] val props = FtpSinkProps(
    output = "/output",
    user = testUtil.ftpServer.writableUser.name,
    password = testUtil.ftpServer.writableUser.password,
    host = testUtil.ftpServer.host,
    port = testUtil.ftpServer.port,
    encode = Some("UTF-8")
  )

  private[this] val ftpClient = FtpClient
    .builder()
    .host(testUtil.ftpServer.host)
    .port(testUtil.ftpServer.port)
    .user(testUtil.ftpServer.writableUser.name)
    .password(testUtil.ftpServer.writableUser.password)
    .build()

  @Before
  def setup(): Unit = {
    if (ftpClient.exist(props.output)) {
      ftpClient.listFileNames(props.output).map(IoUtil.path(props.output, _)).foreach(ftpClient.delete)
      ftpClient.listFileNames(props.output).size shouldBe 0
      ftpClient.delete(props.output)
    }
    ftpClient.mkdir(props.output)

    ftpClient.listFileNames(props.output).size shouldBe 0
  }

  private[this] def setupData(topicName: String): Unit = {
    CloseOnce.doClose(KafkaClient(testUtil.brokers)) { client =>
      if (client.exist(topicName)) client.deleteTopic(topicName)
      client.topicCreator().numberOfPartitions(1).numberOfReplications(1).compacted().create(topicName)
    }

    CloseOnce.doClose(Producer.builder().brokers(testUtil.brokers).build[Array[Byte], Row])(
      _.sender().key(ByteUtil.toBytes("key")).value(data).send(topicName))

    CloseOnce.doClose(
      Consumer.builder().topicName(topicName).offsetFromBegin().brokers(testUtil.brokers).build[Array[Byte], Row]) {
      consumer =>
        val records = consumer.poll(20 seconds, 1)
        val row = records.head.value.get
        row.size shouldBe data.size
        row.cell("a").value shouldBe "abc"
        row.cell("b").value shouldBe 123
        row.cell("c").value shouldBe true
    }
  }
  @Test
  def testNormalCase(): Unit = {
    val topicName = methodName
    val connectorName = methodName
    setupData(topicName)
    testUtil.connectorClient
      .connectorCreator()
      .topic(topicName)
      .connectorClass(classOf[FtpSink])
      .numberOfTasks(1)
      .disableConverter()
      .name(connectorName)
      .schema(schema)
      .config(props.toMap)
      .create()

    try {
      TestFtpUtil.checkConnector(testUtil, connectorName)
      OharaTestUtil.await(() => ftpClient.listFileNames(props.output).size == 1, 10 seconds)
      val lines = ftpClient.readLines(IoUtil.path(props.output, ftpClient.listFileNames(props.output).head))
      lines.length shouldBe 1
      val items = lines.head.split(",")
      items.length shouldBe data.size
      items(0) shouldBe data.cell(0).value.toString
      items(1) shouldBe data.cell(1).value.toString
      items(2) shouldBe data.cell(2).value.toString
    } finally testUtil.connectorClient.delete(methodName)
  }

  @Test
  def testPartialColumns(): Unit = {
    val topicName = methodName
    val connectorName = methodName
    setupData(topicName)
    testUtil.connectorClient
      .connectorCreator()
      .topic(topicName)
      .connectorClass(classOf[FtpSink])
      .numberOfTasks(1)
      .disableConverter()
      .name(connectorName)
      // skip last column
      .schema(schema.slice(0, schema.length - 1))
      .config(props.toMap)
      .create()

    try {
      TestFtpUtil.checkConnector(testUtil, connectorName)
      OharaTestUtil.await(() => ftpClient.listFileNames(props.output).size == 1, 10 seconds)
      val lines = ftpClient.readLines(IoUtil.path(props.output, ftpClient.listFileNames(props.output).head))
      lines.length shouldBe 1
      val items = lines.head.split(",")
      items.length shouldBe data.size - 1
      items(0) shouldBe data.cell(0).value.toString
      items(1) shouldBe data.cell(1).value.toString
    } finally testUtil.connectorClient.delete(methodName)
  }

  @Test
  def testUnmatchedSchema(): Unit = {
    val topicName = methodName
    val connectorName = methodName
    setupData(topicName)
    testUtil.connectorClient
      .connectorCreator()
      .topic(topicName)
      .connectorClass(classOf[FtpSink])
      .numberOfTasks(1)
      .disableConverter()
      .name(connectorName)
      // the name can't be casted to int
      .schema(Seq(Column("name", DataType.INT, 1)))
      .config(props.toMap)
      .create()

    try {
      TestFtpUtil.checkConnector(testUtil, connectorName)
      TimeUnit.SECONDS.sleep(2)
      ftpClient.listFileNames(props.output).size shouldBe 0
    } finally testUtil.connectorClient.delete(methodName)
  }

  @Test
  def testInvalidOutput(): Unit = {
    val topicName = methodName
    val connectorName = methodName
    testUtil.connectorClient
      .connectorCreator()
      .topic(topicName)
      .connectorClass(classOf[FtpSink])
      .numberOfTasks(1)
      .disableConverter()
      .name(connectorName)
      .schema(schema)
      .config(props.copy(output = "/abc").toMap)
      .create()
    TestFtpUtil.assertFailedConnector(testUtil, connectorName)
  }

}
