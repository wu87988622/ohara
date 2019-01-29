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

package com.island.ohara.it
import java.time.Duration

import com.island.ohara.client.FtpClient
import com.island.ohara.client.configurator.v0.ConnectorApi.ConnectorCreationRequest
import com.island.ohara.client.configurator.v0.TopicApi.TopicCreationRequest
import com.island.ohara.client.configurator.v0.{ConnectorApi, InfoApi, TopicApi}
import com.island.ohara.client.kafka.WorkerClient
import com.island.ohara.common.data._
import com.island.ohara.common.util.{CommonUtil, Releasable, VersionUtil}
import com.island.ohara.configurator.Configurator
import com.island.ohara.connector.ftp.{FtpSink, FtpSinkProps, FtpSource, FtpSourceProps}
import com.island.ohara.integration.With3Brokers3Workers
import com.island.ohara.kafka.{Consumer, Producer}
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration._

class TestConfigurator extends With3Brokers3Workers with Matchers {
  private[this] val workerClient = WorkerClient(testUtil.workersConnProps)

  private[this] val configurator =
    Configurator
      .builder()
      .hostname("localhost")
      .port(0)
      .fake(testUtil.brokersConnProps(), testUtil().workersConnProps())
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

    val request = ConnectorCreationRequest(
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
      .connectionProps(testUtil.brokersConnProps)
      .offsetFromBegin()
      .topicName(topic.id)
      .build(Serializer.ROW, Serializer.BYTES)
    try {
      val records = consumer.poll(java.time.Duration.ofSeconds(20), rows.length).asScala

      records.length shouldBe rows.length
      records.head.key.get shouldBe rows.head
      records(1).key.get shouldBe rows(1)
    } finally consumer.close()

    try {
      Await.result(connectorAccess.stop(source.id), 10 seconds)
      Await.result(ConnectorApi.access().hostname(configurator.hostname).port(configurator.port).delete(source.id),
                   10 seconds) shouldBe source
    } finally if (Await.result(workerClient.exist(source.id), 10 seconds))
      Await.result(workerClient.delete(source.id), 10 seconds)

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

    val producer = Producer.builder().connectionProps(testUtil.brokersConnProps).build(Serializer.ROW, Serializer.BYTES)
    try rows.foreach(row => producer.sender().key(row).send(topic.id))
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

      val request = ConnectorCreationRequest(
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
      } finally if (Await.result(workerClient.exist(sink.id), 10 seconds))
        Await.result(workerClient.delete(sink.id), 10 seconds)
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
    Releasable.close(configurator)
  }

}
