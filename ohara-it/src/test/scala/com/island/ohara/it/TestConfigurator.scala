package com.island.ohara.it
import java.time.Duration

import com.island.ohara.client.configurator.v0.ConnectorApi.ConnectorConfigurationRequest
import com.island.ohara.client.configurator.v0.TopicApi.TopicCreationRequest
import com.island.ohara.client.configurator.v0.{ConnectorApi, InfoApi, TopicApi}
import com.island.ohara.client.{ConnectorClient, FtpClient}
import com.island.ohara.common.data._
import com.island.ohara.common.util.{CommonUtil, ReleaseOnce, VersionUtil}
import com.island.ohara.configurator.Configurator
import com.island.ohara.connector.ftp.{FtpSink, FtpSinkProps, FtpSource, FtpSourceProps}
import com.island.ohara.integration.With3Brokers3Workers
import com.island.ohara.kafka.{Consumer, KafkaClient, Producer}
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration._

class TestConfigurator extends With3Brokers3Workers with Matchers {
  private[this] val connectorClient = ConnectorClient(testUtil.workersConnProps)

  private[this] val configurator =
    Configurator
      .builder()
      .hostname("localhost")
      .port(0)
      .kafkaClient(KafkaClient.of(testUtil.brokersConnProps))
      .connectClient(ConnectorClient(testUtil.workersConnProps))
      .build()

  private[this] val connectorAccess = ConnectorApi.access().hostname(configurator.hostname).port(configurator.port)

  private[this] val ftpServer = testUtil.ftpServer

  @Test
  def testRunFtpSource(): Unit = {
    val topic = Await.result(TopicApi
                               .access()
                               .hostname(configurator.hostname)
                               .port(configurator.port)
                               .add(TopicCreationRequest(methodName, 1, 1)),
                             10 seconds)
    val sourceProps = FtpSourceProps(
      inputFolder = "/input",
      completedFolder = "/backup",
      errorFolder = "/error",
      user = testUtil.ftpServer.user,
      password = testUtil.ftpServer.password,
      hostname = testUtil.ftpServer.hostname,
      port = testUtil.ftpServer.port,
      encode = Some("UTF-8")
    )

    val rows: Seq[Row] = Seq(
      Row.of(Cell.of("name", "chia"), Cell.of("ranking", 1), Cell.of("single", false)),
      Row.of(Cell.of("name", "jack"), Cell.of("ranking", 99), Cell.of("single", true))
    )
    val header: String = rows.head.cells().asScala.map(_.name).mkString(",")
    val data: Seq[String] = rows.map(row => {
      row.cells().asScala.map(_.value.toString).mkString(",")
    })

    // setup env
    val ftpClient = FtpClient
      .builder()
      .hostname(ftpServer.hostname)
      .port(ftpServer.port)
      .user(ftpServer.user)
      .password(ftpServer.password)
      .build()
    try {
      TestFtp2Ftp.rebuild(ftpClient, sourceProps.inputFolder)
      TestFtp2Ftp.rebuild(ftpClient, sourceProps.completedFolder.get)
      TestFtp2Ftp.rebuild(ftpClient, sourceProps.errorFolder)
      TestFtp2Ftp.setupInput(ftpClient, sourceProps, header, data)
    } finally ftpClient.close()

    val request = ConnectorConfigurationRequest(
      name = methodName,
      className = classOf[FtpSource].getName,
      schema = Seq(
        Column.of("name", DataType.STRING, 1),
        Column.of("ranking", DataType.INT, 2),
        Column.of("single", DataType.BOOLEAN, 3)
      ),
      topics = Seq(topic.id),
      numberOfTasks = 1,
      configs = sourceProps.toMap
    )

    val source =
      Await
        .result(ConnectorApi.access().hostname(configurator.hostname).port(configurator.port).add(request), 10 seconds)
    Await.result(connectorAccess.start(source.id), 10 seconds)

    val consumer = Consumer
      .builder()
      .brokers(testUtil.brokersConnProps)
      .offsetFromBegin()
      .topicName(topic.id)
      .build(Serializer.BYTES, Serializer.ROW)
    try {
      val records = consumer.poll(java.time.Duration.ofSeconds(20), rows.length).asScala

      records.length shouldBe rows.length
      records.head.value.get shouldBe rows.head
      records(1).value.get shouldBe rows(1)
    } finally consumer.close()

    try {
      Await.result(connectorAccess.stop(source.id), 10 seconds)
      Await.result(ConnectorApi.access().hostname(configurator.hostname).port(configurator.port).delete(source.id),
                   10 seconds) shouldBe source
    } finally if (connectorClient.exist(source.id)) connectorClient.delete(source.id)

  }

  @Test
  def testRunFtpSink(): Unit = {
    val topic = Await.result(TopicApi
                               .access()
                               .hostname(configurator.hostname)
                               .port(configurator.port)
                               .add(TopicCreationRequest(methodName, 1, 1)),
                             10 seconds)
    val sinkProps = FtpSinkProps(
      output = "/backup",
      needHeader = false,
      user = testUtil.ftpServer.user,
      password = testUtil.ftpServer.password,
      hostname = testUtil.ftpServer.hostname,
      port = testUtil.ftpServer.port,
      encode = Some("UTF-8")
    )

    val rows: Seq[Row] = Seq(
      Row.of(Cell.of("name", "chia"), Cell.of("ranking", 1), Cell.of("single", false)),
      Row.of(Cell.of("name", "jack"), Cell.of("ranking", 99), Cell.of("single", true))
    )
    val data: Seq[String] = rows.map(row => {
      row.cells().asScala.map(_.value.toString).mkString(",")
    })

    val producer = Producer.builder().brokers(testUtil.brokersConnProps).build(Serializer.BYTES, Serializer.ROW)
    try rows.foreach(row => producer.sender().value(row).send(topic.id))
    finally producer.close()

    // setup env
    val ftpClient = FtpClient
      .builder()
      .hostname(ftpServer.hostname)
      .port(ftpServer.port)
      .user(ftpServer.user)
      .password(ftpServer.password)
      .build()
    try {
      TestFtp2Ftp.rebuild(ftpClient, sinkProps.output)

      val request = ConnectorConfigurationRequest(
        name = methodName,
        className = classOf[FtpSink].getName,
        schema = Seq.empty,
        topics = Seq(topic.id),
        numberOfTasks = 1,
        configs = sinkProps.toMap
      )

      val sink = Await
        .result(ConnectorApi.access().hostname(configurator.hostname).port(configurator.port).add(request), 10 seconds)
      Await.result(connectorAccess.start(sink.id), 10 seconds)
      try {
        CommonUtil.await(() => ftpClient.listFileNames(sinkProps.output).nonEmpty, Duration.ofSeconds(30))
        ftpClient
          .listFileNames(sinkProps.output)
          .foreach(name => {
            val lines = ftpClient.readLines(com.island.ohara.common.util.CommonUtil.path(sinkProps.output, name))
            lines.length shouldBe 2
            lines(0) shouldBe data.head
            lines(1) shouldBe data(1)
          })
        Await.result(connectorAccess.stop(sink.id), 10 seconds)
        Await.result(ConnectorApi.access().hostname(configurator.hostname).port(configurator.port).delete(sink.id),
                     10 seconds) shouldBe sink
      } finally if (connectorClient.exist(sink.id)) connectorClient.delete(sink.id)
    } finally ftpClient.close()
  }

  @Test
  def testCluster(): Unit = {
    val cluster =
      Await.result(InfoApi.access().hostname(configurator.hostname).port(configurator.port).get(), 10 seconds)
    cluster.sources
      .filter(_.className.startsWith("com.island"))
      .foreach(s => {
        s.version shouldBe VersionUtil.VERSION
        s.revision shouldBe VersionUtil.REVISION
      })
    cluster.sinks
      .filter(_.className.startsWith("com.island"))
      .foreach(s => {
        s.version shouldBe VersionUtil.VERSION
        s.revision shouldBe VersionUtil.REVISION
      })
  }

  @After
  def tearDown(): Unit = {
    ReleaseOnce.close(connectorClient)
    ReleaseOnce.close(configurator)
  }

}
