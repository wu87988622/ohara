package com.island.ohara.it
import com.island.ohara.client.ConfiguratorJson.{Column, Sink, SinkRequest, Source, SourceRequest}
import com.island.ohara.client.{ConfiguratorClient, ConnectorClient, FtpClient}
import com.island.ohara.configurator.Configurator
import com.island.ohara.configurator.store.Store
import com.island.ohara.connector.ftp.{FtpSinkProps, FtpSourceProps}
import com.island.ohara.data.{Cell, Row}
import com.island.ohara.integration.{OharaTestUtil, With3Brokers3Workers}
import com.island.ohara.io.CloseOnce._
import com.island.ohara.io.{CloseOnce, IoUtil}
import com.island.ohara.kafka.{Consumer, KafkaClient, KafkaUtil, Producer}
import com.island.ohara.serialization.DataType
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.duration._

class TestConfigurator extends With3Brokers3Workers with Matchers {
  private[this] val configurator =
    Configurator
      .builder()
      .hostname("localhost")
      .port(0)
      .store(Store.builder().topicName(random()).brokers(testUtil.brokers).buildBlocking[String, Any])
      .kafkaClient(KafkaClient(testUtil.brokers))
      .connectClient(ConnectorClient(testUtil.workers))
      .build()

  private[this] val client = ConfiguratorClient(configurator.hostname, configurator.port)

  private[this] val ftpServer = testUtil.ftpServer

  @Test
  def testRunFtpSource(): Unit = {
    val topicName = methodName
    val sourceProps = FtpSourceProps(
      input = "/input",
      output = "/backup",
      error = "/error",
      user = testUtil.ftpServer.writableUser.name,
      password = testUtil.ftpServer.writableUser.password,
      host = testUtil.ftpServer.host,
      port = testUtil.ftpServer.port,
      encode = Some("UTF-8")
    )

    val rows: Seq[Row] = Seq(
      Row(Cell("name", "chia"), Cell("ranking", 1), Cell("single", false)),
      Row(Cell("name", "jack"), Cell("ranking", 99), Cell("single", true))
    )
    val header: String = rows.head.map(_.name).mkString(",")
    val data: Seq[String] = rows.map(row => {
      row.map(_.value.toString).mkString(",")
    })

    // setup env
    doClose(
      FtpClient
        .builder()
        .host(ftpServer.host)
        .port(ftpServer.port)
        .user(ftpServer.writableUser.name)
        .password(ftpServer.writableUser.password)
        .build()) { ftpClient =>
      TestFtp2Ftp.rebuild(ftpClient, sourceProps.input)
      TestFtp2Ftp.rebuild(ftpClient, sourceProps.output)
      TestFtp2Ftp.rebuild(ftpClient, sourceProps.error)
      TestFtp2Ftp.setupInput(ftpClient, sourceProps, header, data)
    }

    val request = SourceRequest(
      name = methodName,
      className = "ftp",
      schema = Seq(
        Column("name", DataType.STRING, 1),
        Column("ranking", DataType.INT, 2),
        Column("single", DataType.BOOLEAN, 3)
      ),
      topics = Seq(topicName),
      numberOfTasks = 1,
      configs = sourceProps.toMap
    )

    val source = client.add[SourceRequest, Source](request)
    client.start[Source](source.uuid)

    try {
      doClose(
        Consumer.builder().brokers(testUtil.brokers).offsetFromBegin().topicName(topicName).build[Array[Byte], Row]) {
        consumer =>
          val records = consumer.poll(20 seconds, rows.length)
          records.length shouldBe rows.length
          records(0).value.get shouldBe rows(0)
          records(1).value.get shouldBe rows(1)
      }
      client.stop[Source](source.uuid)
      client.delete[Source](source.uuid)
    } finally if (testUtil.connectorClient.exist(source.uuid)) testUtil.connectorClient.delete(source.uuid)
  }

  @Test
  def testRunFtpSink(): Unit = {
    val topicName = methodName
    val sinkProps = FtpSinkProps(
      output = "/backup",
      needHeader = false,
      user = testUtil.ftpServer.writableUser.name,
      password = testUtil.ftpServer.writableUser.password,
      host = testUtil.ftpServer.host,
      port = testUtil.ftpServer.port,
      encode = Some("UTF-8")
    )

    val rows: Seq[Row] = Seq(
      Row(Cell("name", "chia"), Cell("ranking", 1), Cell("single", false)),
      Row(Cell("name", "jack"), Cell("ranking", 99), Cell("single", true))
    )
    val data: Seq[String] = rows.map(row => {
      row.map(_.value.toString).mkString(",")
    })

    KafkaUtil.createTopic(testUtil.brokers, topicName, 1, 1)

    doClose(Producer.builder().brokers(testUtil.brokers).build[Array[Byte], Row]) { producer =>
      rows.foreach(row => producer.sender().value(row).send(topicName))
    }

    // setup env
    doClose(
      FtpClient
        .builder()
        .host(ftpServer.host)
        .port(ftpServer.port)
        .user(ftpServer.writableUser.name)
        .password(ftpServer.writableUser.password)
        .build()) { ftpClient =>
      TestFtp2Ftp.rebuild(ftpClient, sinkProps.output)

      val request = SinkRequest(
        name = methodName,
        className = "ftp",
        schema = Seq.empty,
        topics = Seq(topicName),
        numberOfTasks = 1,
        configs = sinkProps.toMap
      )

      val sink = client.add[SinkRequest, Sink](request)
      client.start[Sink](sink.uuid)
      try {
        OharaTestUtil.await(() => ftpClient.listFileNames(sinkProps.output).nonEmpty, 30 seconds)
        ftpClient
          .listFileNames(sinkProps.output)
          .foreach(name => {
            val lines = ftpClient.readLines(IoUtil.path(sinkProps.output, name))
            lines.length shouldBe 2
            lines(0) shouldBe data(0)
            lines(1) shouldBe data(1)
          })
        client.stop[Sink](sink.uuid)
        client.delete[Sink](sink.uuid)
      } finally if (testUtil.connectorClient.exist(sink.uuid)) testUtil.connectorClient.delete(sink.uuid)
    }
  }

  @After
  def tearDown(): Unit = {
    CloseOnce.close(client)
    CloseOnce.close(configurator)
  }

}
