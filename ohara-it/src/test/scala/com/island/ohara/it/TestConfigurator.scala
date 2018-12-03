package com.island.ohara.it
import java.time.Duration

import com.island.ohara.client.ConfiguratorJson.{
  ClusterInformation,
  Column,
  Sink,
  SinkRequest,
  Source,
  SourceRequest,
  TopicInfo,
  TopicInfoRequest
}
import com.island.ohara.client.{ConfiguratorClient, ConnectorClient, FtpClient}
import com.island.ohara.common.data.{Cell, DataType, Row, Serializer}
import com.island.ohara.common.util.{CloseOnce, CommonUtil, VersionUtil}
import com.island.ohara.configurator.Configurator
import com.island.ohara.configurator.store.Store
import com.island.ohara.connector.ftp.{FtpSinkProps, FtpSourceProps}
import com.island.ohara.integration.With3Brokers3Workers
import com.island.ohara.kafka.{Consumer, KafkaClient, Producer}
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.collection.JavaConverters._

class TestConfigurator extends With3Brokers3Workers with Matchers {
  private[this] val connectorClient = ConnectorClient(testUtil.workersConnProps)

  private[this] val configurator =
    Configurator
      .builder()
      .hostname("localhost")
      .port(0)
      .store(
        Store
          .builder()
          .topicName(random())
          .brokers(testUtil.brokersConnProps)
          .build(Serializer.STRING, Serializer.OBJECT))
      .kafkaClient(KafkaClient.of(testUtil.brokersConnProps))
      .connectClient(ConnectorClient(testUtil.workersConnProps))
      .build()

  private[this] val client = ConfiguratorClient(configurator.hostname, configurator.port)

  private[this] val ftpServer = testUtil.ftpServer

  @Test
  def testRunFtpSource(): Unit = {
    val topic = client.add[TopicInfoRequest, TopicInfo](TopicInfoRequest(methodName, 1, 1))
    val sourceProps = FtpSourceProps(
      inputFolder = "/input",
      completedFolder = "/backup",
      errorFolder = "/error",
      user = testUtil.ftpServer.user,
      password = testUtil.ftpServer.password,
      host = testUtil.ftpServer.host,
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
      .hostname(ftpServer.host)
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

    val request = SourceRequest(
      name = methodName,
      className = "ftp",
      schema = Seq(
        Column("name", DataType.STRING, 1),
        Column("ranking", DataType.INT, 2),
        Column("single", DataType.BOOLEAN, 3)
      ),
      topics = Seq(topic.uuid),
      numberOfTasks = 1,
      configs = sourceProps.toMap
    )

    val source = client.add[SourceRequest, Source](request)
    client.start[Source](source.uuid)

    val consumer = Consumer
      .builder()
      .brokers(testUtil.brokersConnProps)
      .offsetFromBegin()
      .topicName(topic.uuid)
      .build(Serializer.BYTES, Serializer.ROW)
    try {
      val records = consumer.poll(java.time.Duration.ofSeconds(20), rows.length).asScala

      records.length shouldBe rows.length
      records(0).value.get shouldBe rows(0)
      records(1).value.get shouldBe rows(1)
    } finally consumer.close()

    try {
      client.stop[Source](source.uuid)
      client.delete[Source](source.uuid)
    } finally if (connectorClient.exist(source.uuid)) connectorClient.delete(source.uuid)

  }

  @Test
  def testRunFtpSink(): Unit = {
    val topic = client.add[TopicInfoRequest, TopicInfo](TopicInfoRequest(methodName, 1, 1))
    val sinkProps = FtpSinkProps(
      output = "/backup",
      needHeader = false,
      user = testUtil.ftpServer.user,
      password = testUtil.ftpServer.password,
      host = testUtil.ftpServer.host,
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
    try rows.foreach(row => producer.sender().value(row).send(topic.uuid))
    finally producer.close()

    // setup env
    val ftpClient = FtpClient
      .builder()
      .hostname(ftpServer.host)
      .port(ftpServer.port)
      .user(ftpServer.user)
      .password(ftpServer.password)
      .build()
    try {
      TestFtp2Ftp.rebuild(ftpClient, sinkProps.output)

      val request = SinkRequest(
        name = methodName,
        className = "ftp",
        schema = Seq.empty,
        topics = Seq(topic.uuid),
        numberOfTasks = 1,
        configs = sinkProps.toMap
      )

      val sink = client.add[SinkRequest, Sink](request)
      client.start[Sink](sink.uuid)
      try {
        CommonUtil.await(() => ftpClient.listFileNames(sinkProps.output).nonEmpty, Duration.ofSeconds(30))
        ftpClient
          .listFileNames(sinkProps.output)
          .foreach(name => {
            val lines = ftpClient.readLines(com.island.ohara.common.util.CommonUtil.path(sinkProps.output, name))
            lines.length shouldBe 2
            lines(0) shouldBe data(0)
            lines(1) shouldBe data(1)
          })
        client.stop[Sink](sink.uuid)
        client.delete[Sink](sink.uuid)
      } finally if (connectorClient.exist(sink.uuid)) connectorClient.delete(sink.uuid)
    } finally ftpClient.close()
  }

  @Test
  def testCluster(): Unit = {
    val cluster = client.cluster[ClusterInformation]
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
    CloseOnce.close(connectorClient)
    CloseOnce.close(client)
    CloseOnce.close(configurator)
  }

}
