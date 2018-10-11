package com.island.ohara.it
import com.island.ohara.client.ConfiguratorJson.{Column, Source, SourceRequest}
import com.island.ohara.client.{ConfiguratorClient, ConnectorClient, FtpClient}
import com.island.ohara.configurator.Configurator
import com.island.ohara.configurator.store.Store
import com.island.ohara.connector.ftp.FtpSourceProps
import com.island.ohara.data.{Cell, Row}
import com.island.ohara.integration.With3Brokers3Workers
import com.island.ohara.io.CloseOnce
import com.island.ohara.io.CloseOnce._
import com.island.ohara.kafka.{Consumer, KafkaClient}
import com.island.ohara.serialization.DataType
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.duration._

class TestConfigurator extends With3Brokers3Workers with Matchers {
  private[this] val topicName = random()

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
        .build()) { client =>
      TestFtp2Ftp.rebuild(client, sourceProps.input)
      TestFtp2Ftp.rebuild(client, sourceProps.output)
      TestFtp2Ftp.rebuild(client, sourceProps.error)
      TestFtp2Ftp.setupInput(client, sourceProps, header, data)
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

  @After
  def tearDown(): Unit = {
    CloseOnce.close(client)
    CloseOnce.close(configurator)
  }

}
