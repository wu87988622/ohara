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

import java.io.{BufferedWriter, OutputStreamWriter}
import java.time.Duration

import com.island.ohara.client.filesystem.FileSystem
import com.island.ohara.client.kafka.WorkerClient
import com.island.ohara.common.data.{Cell, Column, DataType, Row}
import com.island.ohara.common.setting.{ConnectorKey, TopicKey}
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.testing.With3Brokers3Workers
import org.junit.{After, Before, Test}
import org.scalatest.Matchers._

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * ftp csv -> topic -> ftp csv
  */
class TestFtp2Ftp extends With3Brokers3Workers {
  private[this] val workerClient = WorkerClient(testUtil.workersConnProps)

  private[this] val schema: Seq[Column] = Seq(
    Column.builder().name("name").dataType(DataType.STRING).order(1).build(),
    Column.builder().name("ranking").dataType(DataType.INT).order(2).build(),
    Column.builder().name("single").dataType(DataType.BOOLEAN).order(3).build()
  )

  private[this] val row = Row.of(Cell.of("name", "chia"), Cell.of("ranking", 1), Cell.of("single", false))
  private[this] val header: String = row.cells().asScala.map(_.name).mkString(",")
  private[this] val data = (1 to 1000).map(_ => row.cells().asScala.map(_.value.toString).mkString(","))

  private[this] val fileSystem = FileSystem.ftpBuilder
    .hostname(testUtil.ftpServer.hostname)
    .port(testUtil.ftpServer.port)
    .user(testUtil.ftpServer.user)
    .password(testUtil.ftpServer.password)
    .build()

  private[this] val sourceProps = FtpSourceProps(
    inputFolder = "/input",
    completedFolder = Some("/backup"),
    errorFolder = "/error",
    user = testUtil.ftpServer.user,
    password = testUtil.ftpServer.password,
    hostname = testUtil.ftpServer.hostname,
    port = testUtil.ftpServer.port,
    encode = "UTF-8"
  )

  private[this] val sinkProps = FtpSinkProps(
    topicsDir = "/output",
    needHeader = true,
    user = testUtil.ftpServer.user,
    password = testUtil.ftpServer.password,
    hostname = testUtil.ftpServer.hostname,
    port = testUtil.ftpServer.port,
    encode = "UTF-8"
  )

  @Before
  def setup(): Unit = {
    TestFtp2Ftp.rebuild(fileSystem, sourceProps.inputFolder)
    TestFtp2Ftp.rebuild(fileSystem, sourceProps.completedFolder.get)
    TestFtp2Ftp.rebuild(fileSystem, sourceProps.errorFolder)
    TestFtp2Ftp.rebuild(fileSystem, sinkProps.topicsDir)
    TestFtp2Ftp.setupInput(fileSystem, sourceProps, header, data)
  }

  @Test
  def testNormalCase(): Unit = {
    val topicKey = TopicKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    val sinkConnectorKey = ConnectorKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    val sourceConnectorKey = ConnectorKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    // start sink
    Await.result(
      workerClient
        .connectorCreator()
        .topicKey(topicKey)
        .connectorClass(classOf[FtpSink])
        .numberOfTasks(1)
        .connectorKey(sinkConnectorKey)
        .columns(schema)
        .settings(sinkProps.toMap)
        .create(),
      10 seconds
    )

    try {
      try {
        Await.result(
          workerClient
            .connectorCreator()
            .topicKey(topicKey)
            .connectorClass(classOf[FtpSource])
            .numberOfTasks(1)
            .connectorKey(sourceConnectorKey)
            .columns(schema)
            .settings(sourceProps.toMap)
            .create(),
          10 seconds
        )
        CommonUtils
          .await(() => fileSystem.listFileNames(sourceProps.inputFolder).asScala.isEmpty, Duration.ofSeconds(30))
        CommonUtils.await(() => fileSystem.listFileNames(sourceProps.completedFolder.get).asScala.size == 1,
                          Duration.ofSeconds(30))
        val committedFolder = CommonUtils.path(sinkProps.topicsDir, topicKey.topicNameOnKafka(), "partition0")
        CommonUtils.await(() => {
          if (fileSystem.exists(committedFolder))
            listCommittedFiles(committedFolder).size == 1
          else false
        }, Duration.ofSeconds(30))
        val lines =
          fileSystem.readLines(
            com.island.ohara.common.util.CommonUtils
              .path(committedFolder, fileSystem.listFileNames(committedFolder).asScala.toSeq.head))
        lines.length shouldBe data.length + 1 // header
        lines(0) shouldBe header
        lines(1) shouldBe data.head
        lines(2) shouldBe data(1)
      } finally workerClient.delete(sourceConnectorKey)
    } finally workerClient.delete(sinkConnectorKey)
  }

  private[this] def listCommittedFiles(dir: String): Seq[String] = {
    fileSystem.listFileNames(dir, (fileName: String) => !fileName.contains("_tmp"))
  }

  @After
  def tearDown(): Unit = {
    Releasable.close(fileSystem)
  }
}

object TestFtp2Ftp {

  /**
    * delete all stuffs in the path and then recreate it as a folder
    * @param fileSystem ftp client
    * @param path path on ftp server
    */
  def rebuild(fileSystem: FileSystem, path: String): Unit = {
    if (fileSystem.exists(path)) {
      fileSystem
        .listFileNames(path)
        .asScala
        .map(com.island.ohara.common.util.CommonUtils.path(path, _))
        .foreach(fileSystem.delete)
      fileSystem.listFileNames(path).asScala.size shouldBe 0
      fileSystem.delete(path)
    }
    fileSystem.mkdirs(path)
  }

  def setupInput(fileSystem: FileSystem, props: FtpSourceProps, header: String, data: Seq[String]): Unit = {
    val writer = new BufferedWriter(
      new OutputStreamWriter(
        fileSystem.create(com.island.ohara.common.util.CommonUtils.path(props.inputFolder, "abc"))))
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
