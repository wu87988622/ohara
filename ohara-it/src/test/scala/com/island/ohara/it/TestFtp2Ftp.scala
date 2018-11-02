package com.island.ohara.it
import java.io.{BufferedWriter, OutputStreamWriter}

import com.island.ohara.client.ConfiguratorJson.Column
import com.island.ohara.client.FtpClient
import com.island.ohara.connector.ftp.{FtpSink, FtpSinkProps, FtpSource, FtpSourceProps}
import com.island.ohara.data.{Cell, Row}
import com.island.ohara.integration.{OharaTestUtil, With3Brokers3Workers}
import com.island.ohara.io.{CloseOnce, IoUtil}
import com.island.ohara.serialization.DataType
import org.junit.{After, Before, Test}
import org.scalatest.Matchers

import scala.concurrent.duration._

/**
  * ftp csv -> topic -> ftp csv
  */
class TestFtp2Ftp extends With3Brokers3Workers with Matchers {

  private[this] val schema: Seq[Column] = Seq(
    Column("name", DataType.STRING, 1),
    Column("ranking", DataType.INT, 2),
    Column("single", DataType.BOOLEAN, 3)
  )
  private[this] val rows: Seq[Row] = Seq(
    Row(Cell("name", "chia"), Cell("ranking", 1), Cell("single", false)),
    Row(Cell("name", "jack"), Cell("ranking", 99), Cell("single", true))
  )
  private[this] val header: String = rows.head.map(_.name).mkString(",")
  private[this] val data: Seq[String] = rows.map(row => {
    row.map(_.value.toString).mkString(",")
  })
  private[this] val ftpClient = FtpClient
    .builder()
    .host(testUtil.ftpServer.host)
    .port(testUtil.ftpServer.port)
    .user(testUtil.ftpServer.user)
    .password(testUtil.ftpServer.password)
    .build()

  private[this] val sourceProps = FtpSourceProps(
    inputFolder = "/input",
    completedFolder = "/backup",
    errorFolder = "/error",
    user = testUtil.ftpServer.user,
    password = testUtil.ftpServer.password,
    host = testUtil.ftpServer.host,
    port = testUtil.ftpServer.port,
    encode = Some("UTF-8")
  )

  private[this] val sinkProps = FtpSinkProps(
    output = "/output",
    needHeader = true,
    user = testUtil.ftpServer.user,
    password = testUtil.ftpServer.password,
    host = testUtil.ftpServer.host,
    port = testUtil.ftpServer.port,
    encode = Some("UTF-8")
  )

  @Before
  def setup(): Unit = {
    TestFtp2Ftp.rebuild(ftpClient, sourceProps.inputFolder)
    TestFtp2Ftp.rebuild(ftpClient, sourceProps.completedFolder)
    TestFtp2Ftp.rebuild(ftpClient, sourceProps.errorFolder)
    TestFtp2Ftp.rebuild(ftpClient, sinkProps.output)
    TestFtp2Ftp.setupInput(ftpClient, sourceProps, header, data)
  }

  @Test
  def testNormalCase(): Unit = {
    val topicName = methodName
    val sinkName = methodName + "-sink"
    val sourceName = methodName + "-source"
    // start sink
    testUtil.connectorClient
      .connectorCreator()
      .topic(topicName)
      .connectorClass(classOf[FtpSink])
      .numberOfTasks(1)
      .disableConverter()
      .name(sinkName)
      .schema(schema)
      .configs(sinkProps.toMap)
      .create()

    try {
      try {
        testUtil.connectorClient
          .connectorCreator()
          .topic(topicName)
          .connectorClass(classOf[FtpSource])
          .numberOfTasks(1)
          .disableConverter()
          .name(sourceName)
          .schema(schema)
          .configs(sourceProps.toMap)
          .create()
        OharaTestUtil.await(() => ftpClient.listFileNames(sourceProps.inputFolder).isEmpty, 30 seconds)
        OharaTestUtil.await(() => ftpClient.listFileNames(sourceProps.completedFolder).size == 1, 30 seconds)
        OharaTestUtil.await(() => ftpClient.listFileNames(sinkProps.output).size == 1, 30 seconds)
        val lines = ftpClient.readLines(IoUtil.path(sinkProps.output, ftpClient.listFileNames(sinkProps.output).head))
        lines.length shouldBe rows.length + 1 // header
        lines(0) shouldBe header
        lines(1) shouldBe data(0)
        lines(2) shouldBe data(1)
      } finally testUtil.connectorClient.delete(sourceName)
    } finally testUtil.connectorClient.delete(sinkName)
  }

  @After
  def tearDown(): Unit = {
    CloseOnce.close(ftpClient)
  }
}

private[it] object TestFtp2Ftp extends Matchers {

  /**
    * delete all stuffs in the path and then recreate it as a folder
    * @param ftpClient ftp client
    * @param path path on ftp server
    */
  def rebuild(ftpClient: FtpClient, path: String): Unit = {
    if (ftpClient.exist(path)) {
      ftpClient.listFileNames(path).map(IoUtil.path(path, _)).foreach(ftpClient.delete)
      ftpClient.listFileNames(path).size shouldBe 0
      ftpClient.delete(path)
    }
    ftpClient.mkdir(path)
  }

  def setupInput(ftpClient: FtpClient, props: FtpSourceProps, header: String, data: Seq[String]): Unit = {
    val writer = new BufferedWriter(new OutputStreamWriter(ftpClient.create(IoUtil.path(props.inputFolder, "abc"))))
    try {
      writer.append(header)
      writer.newLine()
      data.foreach(line => {
        writer.append(line)
        writer.newLine()
      })
    } finally writer.close()
  }
}
