package com.island.ohara.connector.ftp
import java.io.{BufferedWriter, OutputStreamWriter}

import com.island.ohara.client.ConfiguratorJson.Column
import com.island.ohara.data.{Cell, Row}
import com.island.ohara.integration.With3Brokers3Workers
import com.island.ohara.io.{CloseOnce, IoUtil}
import com.island.ohara.kafka.Consumer
import com.island.ohara.serialization.DataType
import org.junit.{After, Before, Test}
import org.scalatest.Matchers

import scala.concurrent.duration._

class TestFtpSource extends With3Brokers3Workers with Matchers {
  private[this] val schema: Seq[Column] = Seq(
    Column("name", DataType.STRING, 1),
    Column("ranking", DataType.INT, 2),
    Column("single", DataType.BOOLEAN, 3)
  )
  private[this] val rows: Seq[Row] = Seq(
    Row(Cell("name", "chia"), Cell("ranking", 1), Cell("single", false)),
    Row(Cell("name", "jack"), Cell("ranking", 99), Cell("single", true))
  )
  private[this] val data: Seq[String] = rows.map(row => {
    row.map(_.value.toString).mkString(",")
  })
  private[this] val ftpClient = FtpClient
    .builder()
    .host(testUtil.ftpServer.host)
    .port(testUtil.ftpServer.port)
    .user(testUtil.ftpServer.writableUser.name)
    .password(testUtil.ftpServer.writableUser.password)
    .build()

  private[this] val props = FtpSourceProps(
    input = "/input",
    output = "/output",
    error = "/error",
    user = testUtil.ftpServer.writableUser.name,
    password = testUtil.ftpServer.writableUser.password,
    host = testUtil.ftpServer.host,
    port = testUtil.ftpServer.port,
    encode = Some("UTF-8")
  )

  @Before
  def setup(): Unit = {
    def rebuild(path: String): Unit = {
      if (ftpClient.exist(path)) {
        ftpClient.listFileNames(path).map(IoUtil.path(path, _)).foreach(ftpClient.delete)
        ftpClient.listFileNames(path).size shouldBe 0
        ftpClient.delete(path)
      }
      ftpClient.mkdir(path)
    }
    // cleanup all files in order to avoid corrupted files
    rebuild(props.input)
    rebuild(props.error)
    rebuild(props.output)

    val writer = new BufferedWriter(new OutputStreamWriter(ftpClient.create(IoUtil.path(props.input, "abc"))))
    try {
      data.foreach(line => {
        writer.append(line)
        writer.newLine()
      })
    } finally writer.close()

    ftpClient.listFileNames(props.input).isEmpty shouldBe false
  }

  @Test
  def testNormalCase(): Unit = {
    val topicName = methodName
    val connectorName = methodName
    testUtil.connectorClient
      .connectorCreator()
      .topic(topicName)
      .connectorClass(classOf[FtpSource])
      .numberOfTasks(1)
      .disableConverter()
      .name(connectorName)
      .schema(schema)
      .config(props.toMap)
      .create()
    try {
      TestFtpUtil.checkConnector(testUtil, connectorName)
      val consumer =
        Consumer.builder().topicName(topicName).offsetFromBegin().brokers(testUtil.brokers).build[Array[Byte], Row]
      val records = consumer.poll(20 seconds, data.length)
      records.size shouldBe data.length
      val row0 = records(0).value.get
      row0.size shouldBe 3
      row0.cell(0) shouldBe rows(0).cell(0)
      row0.cell(1) shouldBe rows(0).cell(1)
      row0.cell(2) shouldBe rows(0).cell(2)
      val row1 = records(1).value.get
      row1.size shouldBe 3
      row1.cell(0) shouldBe rows(1).cell(0)
      row1.cell(1) shouldBe rows(1).cell(1)
      row1.cell(2) shouldBe rows(1).cell(2)
    } finally testUtil.connectorClient.delete(methodName)
    ftpClient.listFileNames(props.input).size shouldBe 0
    ftpClient.listFileNames(props.output).size shouldBe 1
    ftpClient.listFileNames(props.error).size shouldBe 0
  }

  @Test
  def testPartialColumns(): Unit = {
    val topicName = methodName
    val connectorName = methodName
    testUtil.connectorClient
      .connectorCreator()
      .topic(topicName)
      .connectorClass(classOf[FtpSource])
      .numberOfTasks(1)
      .disableConverter()
      .name(connectorName)
      // skip last column
      .schema(schema.slice(0, schema.length - 1))
      .config(props.toMap)
      .create()
    try {
      TestFtpUtil.checkConnector(testUtil, connectorName)
      val consumer =
        Consumer.builder().topicName(topicName).offsetFromBegin().brokers(testUtil.brokers).build[Array[Byte], Row]
      val records = consumer.poll(20 seconds, data.length)
      records.size shouldBe data.length
      val row0 = records(0).value.get
      row0.size shouldBe 2
      row0.cell(0) shouldBe rows(0).cell(0)
      row0.cell(1) shouldBe rows(0).cell(1)
      val row1 = records(1).value.get
      row1.size shouldBe 2
      row1.cell(0) shouldBe rows(1).cell(0)
      row1.cell(1) shouldBe rows(1).cell(1)
    } finally testUtil.connectorClient.delete(methodName)
    ftpClient.listFileNames(props.input).size shouldBe 0
    ftpClient.listFileNames(props.output).size shouldBe 1
    ftpClient.listFileNames(props.error).size shouldBe 0
  }

  @Test
  def testUnmatchedSchema(): Unit = {
    val topicName = methodName
    val connectorName = methodName
    testUtil.connectorClient
      .connectorCreator()
      .topic(topicName)
      .connectorClass(classOf[FtpSource])
      .numberOfTasks(1)
      .disableConverter()
      .name(connectorName)
      // the name can't be casted to int
      .schema(Seq(Column("name", DataType.INT, 1)))
      .config(props.toMap)
      .create()
    try {
      TestFtpUtil.checkConnector(testUtil, connectorName)
      val consumer =
        Consumer.builder().topicName(topicName).offsetFromBegin().brokers(testUtil.brokers).build[Array[Byte], Row]
      val records = consumer.poll(10 seconds, data.length)
      records.size shouldBe 0
    } finally testUtil.connectorClient.delete(methodName)
    ftpClient.listFileNames(props.input).size shouldBe 0
    ftpClient.listFileNames(props.output).size shouldBe 0
    ftpClient.listFileNames(props.error).size shouldBe 1
  }

  @Test
  def testInvalidInput(): Unit = {
    val topicName = methodName
    val connectorName = methodName
    testUtil.connectorClient
      .connectorCreator()
      .topic(topicName)
      .connectorClass(classOf[FtpSource])
      .numberOfTasks(1)
      .disableConverter()
      .name(connectorName)
      .schema(schema)
      .config(props.copy(input = "/abc").toMap)
      .create()
    TestFtpUtil.assertFailedConnector(testUtil, connectorName)
  }

  @Test
  def testInvalidOutput(): Unit = {
    val topicName = methodName
    val connectorName = methodName
    testUtil.connectorClient
      .connectorCreator()
      .topic(topicName)
      .connectorClass(classOf[FtpSource])
      .numberOfTasks(1)
      .disableConverter()
      .name(connectorName)
      .schema(schema)
      .config(props.copy(output = "/abc").toMap)
      .create()
    TestFtpUtil.assertFailedConnector(testUtil, connectorName)
  }

  @Test
  def testInvalidError(): Unit = {
    val topicName = methodName
    val connectorName = methodName
    testUtil.connectorClient
      .connectorCreator()
      .topic(topicName)
      .connectorClass(classOf[FtpSource])
      .numberOfTasks(1)
      .disableConverter()
      .name(connectorName)
      .schema(schema)
      .config(props.copy(error = "/abc").toMap)
      .create()
    TestFtpUtil.assertFailedConnector(testUtil, connectorName)
  }

  @Test
  def testInvalidSchema(): Unit = {
    val topicName = methodName
    val connectorName = methodName
    testUtil.connectorClient
      .connectorCreator()
      .topic(topicName)
      .connectorClass(classOf[FtpSource])
      .numberOfTasks(1)
      .disableConverter()
      .name(connectorName)
      .schema(
        Seq(
          // 0 is invalid
          Column("name", DataType.STRING, 0),
          Column("ranking", DataType.INT, 2),
          Column("single", DataType.BOOLEAN, 3)
        ))
      .config(props.toMap)
      .create()
    TestFtpUtil.assertFailedConnector(testUtil, connectorName)
  }

  @After
  def tearDown(): Unit = {
    CloseOnce.close(ftpClient)
  }
}
