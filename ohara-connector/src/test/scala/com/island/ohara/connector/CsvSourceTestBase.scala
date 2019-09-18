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

package com.island.ohara.connector

import java.io.{BufferedWriter, OutputStreamWriter}
import java.time.Duration

import com.island.ohara.client.filesystem.FileSystem
import com.island.ohara.client.kafka.WorkerClient
import com.island.ohara.common.data._
import com.island.ohara.common.setting.{ConnectorKey, TopicKey}
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.kafka.Consumer
import com.island.ohara.kafka.Consumer.Record
import com.island.ohara.kafka.connector.csv.CsvConnector._
import com.island.ohara.kafka.connector.csv.CsvSourceConnector
import com.island.ohara.testing.With3Brokers3Workers
import org.junit.{After, Before, Test}
import org.scalatest.Matchers

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

abstract class CsvSourceTestBase extends With3Brokers3Workers with Matchers {
  private[this] val defaultProps: Map[String, String] = Map(
    INPUT_FOLDER_KEY -> "/input",
    COMPLETED_FOLDER_KEY -> "/completed",
    ERROR_FOLDER_KEY -> "/error",
    FILE_NEED_HEADER_KEY -> "false",
    FILE_ENCODE_KEY -> "UTF-8"
  )
  private[this] val schema: Seq[Column] = Seq(
    Column.builder().name("name").dataType(DataType.STRING).order(1).build(),
    Column.builder().name("ranking").dataType(DataType.INT).order(2).build(),
    Column.builder().name("single").dataType(DataType.BOOLEAN).order(3).build()
  )
  private[this] val rows: Seq[Row] = Seq(
    Row.of(Cell.of("name", "chia"), Cell.of("ranking", 1), Cell.of("single", false)),
    Row.of(Cell.of("name", "jack"), Cell.of("ranking", 99), Cell.of("single", true))
  )
  private[this] val header: String = rows.head.cells().asScala.map(_.name).mkString(",")
  private[this] val data: Seq[String] = rows.map(row => {
    row.cells().asScala.map(_.value.toString).mkString(",")
  })

  private[this] val workerClient = WorkerClient(testUtil.workersConnProps)

  protected val fileSystem: FileSystem

  protected val connectorClass: Class[_ <: CsvSourceConnector]

  protected def setupProps: Map[String, String]

  protected def props: Map[String, String] = defaultProps ++ setupProps

  protected def result[T](f: Future[T]): T = Await.result(f, 10 seconds)

  protected def inputDir: String = props(INPUT_FOLDER_KEY)

  protected def completedDir: String = props(COMPLETED_FOLDER_KEY)

  protected def errorDir: String = props(ERROR_FOLDER_KEY)

  protected def setupConnector(props: Map[String, String], schema: Seq[Column]): (TopicKey, ConnectorKey) =
    setupConnector(props, Some(schema))

  protected def setupConnector(props: Map[String, String], schema: Option[Seq[Column]]): (TopicKey, ConnectorKey) = {
    // create a connector and check its state is running
    val (topicKey, connectorKey) = createConnector(props, schema)
    ConnectorTestUtils.checkConnector(testUtil, connectorKey)
    (topicKey, connectorKey)
  }

  protected def createConnector(props: Map[String, String], schema: Seq[Column]): (TopicKey, ConnectorKey) =
    createConnector(props, Some(schema))

  protected def createConnector(props: Map[String, String], schema: Option[Seq[Column]]): (TopicKey, ConnectorKey) = {
    val topicKey = TopicKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    val connectorKey = ConnectorKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    result({
      val creator = workerClient
        .connectorCreator()
        .topicKey(topicKey)
        .connectorClass(connectorClass)
        .numberOfTasks(1)
        .connectorKey(connectorKey)
        .settings(props)
      if (schema.isDefined) creator.columns(schema.get)
      creator.create()
    })
    (topicKey, connectorKey)
  }

  private[this] def setupInput(): Unit = {
    val writer = new BufferedWriter(new OutputStreamWriter(fileSystem.create(CommonUtils.path(inputDir, "abc"))))
    try {
      writer.append(header)
      writer.newLine()
      data.foreach(line => {
        writer.append(line)
        writer.newLine()
      })
    } finally writer.close()
  }

  protected def pollData(topicKey: TopicKey,
                         timeout: scala.concurrent.duration.Duration = 100 seconds,
                         size: Int = data.length): Seq[Record[Row, Array[Byte]]] = {
    val consumer = Consumer
      .builder()
      .topicName(topicKey.topicNameOnKafka)
      .offsetFromBegin()
      .connectionProps(testUtil.brokersConnProps)
      .keySerializer(Serializer.ROW)
      .valueSerializer(Serializer.BYTES)
      .build()
    try consumer.poll(java.time.Duration.ofNanos(timeout.toNanos), size).asScala
    finally consumer.close()
  }

  protected def checkFileCount(inputCount: Int, outputCount: Int, errorCount: Int): Unit = {
    CommonUtils.await(
      () => {
        fileSystem.listFileNames(inputDir).asScala.size == inputCount &&
        fileSystem.listFileNames(completedDir).asScala.size == outputCount &&
        fileSystem.listFileNames(errorDir).asScala.size == errorCount
      },
      Duration.ofSeconds(20)
    )
  }

  @Before
  def setup(): Unit = {
    // cleanup all files in order to avoid corrupted files
    fileSystem.reMkdirs(inputDir)
    fileSystem.reMkdirs(completedDir)
    fileSystem.reMkdirs(errorDir)
    fileSystem.listFileNames(inputDir).asScala.size shouldBe 0
    fileSystem.listFileNames(completedDir).asScala.size shouldBe 0
    fileSystem.listFileNames(errorDir).asScala.size shouldBe 0
    setupInput()
    fileSystem.listFileNames(inputDir).asScala.isEmpty shouldBe false
  }

  @Test
  def testNormalCase(): Unit = {
    val (topicKey, connectorKey) = setupConnector(props, schema)

    try {
      checkFileCount(0, 1, 0)

      val records = pollData(topicKey)
      records.size shouldBe data.length
      val row0 = records.head.key.get
      row0.size shouldBe 3
      row0.cell(0) shouldBe rows.head.cell(0)
      row0.cell(1) shouldBe rows.head.cell(1)
      row0.cell(2) shouldBe rows.head.cell(2)
      val row1 = records(1).key.get
      row1.size shouldBe 3
      row1.cell(0) shouldBe rows(1).cell(0)
      row1.cell(1) shouldBe rows(1).cell(1)
      row1.cell(2) shouldBe rows(1).cell(2)
    } finally result(workerClient.delete(connectorKey))
  }

  @Test
  def testDuplicateInput(): Unit = {
    val (topicKey, connectorKey) = setupConnector(props, schema)

    try {
      checkFileCount(0, 1, 0)

      var records = pollData(topicKey)
      records.size shouldBe data.length
      val row0 = records.head.key.get
      row0.size shouldBe 3
      row0.cell(0) shouldBe rows.head.cell(0)
      row0.cell(1) shouldBe rows.head.cell(1)
      row0.cell(2) shouldBe rows.head.cell(2)
      val row1 = records(1).key.get
      row1.size shouldBe 3
      row1.cell(0) shouldBe rows(1).cell(0)
      row1.cell(1) shouldBe rows(1).cell(1)
      row1.cell(2) shouldBe rows(1).cell(2)

      // put a duplicate file
      setupInput()
      checkFileCount(0, 2, 0)
      records = pollData(topicKey, 10 second)
      records.size shouldBe data.length

    } finally result(workerClient.delete(connectorKey))
  }

  @Test
  def testColumnRename(): Unit = {
    val newSchema = Seq(
      Column.builder().name("name").newName("newName").dataType(DataType.STRING).order(1).build(),
      Column.builder().name("ranking").newName("newRanking").dataType(DataType.INT).order(2).build(),
      Column.builder().name("single").newName("newSingle").dataType(DataType.BOOLEAN).order(3).build()
    )
    val (topicKey, connectorKey) = setupConnector(props, newSchema)

    try {
      checkFileCount(0, 1, 0)

      val records = pollData(topicKey)
      records.size shouldBe data.length
      val row0 = records.head.key.get
      row0.size shouldBe 3
      row0.cell(0).name shouldBe "newName"
      row0.cell(0).value shouldBe rows.head.cell(0).value
      row0.cell(1).name shouldBe "newRanking"
      row0.cell(1).value shouldBe rows.head.cell(1).value
      row0.cell(2).name shouldBe "newSingle"
      row0.cell(2).value shouldBe rows.head.cell(2).value
      val row1 = records(1).key.get
      row1.size shouldBe 3
      row0.cell(0).name shouldBe "newName"
      row1.cell(0).value shouldBe rows(1).cell(0).value
      row0.cell(1).name shouldBe "newRanking"
      row1.cell(1).value shouldBe rows(1).cell(1).value
      row0.cell(2).name shouldBe "newSingle"
      row1.cell(2).value shouldBe rows(1).cell(2).value
    } finally result(workerClient.delete(connectorKey))
  }

  @Test
  def testObjectType(): Unit = {
    val newSchema = Seq(
      Column.builder().name("name").dataType(DataType.OBJECT).order(1).build(),
      Column.builder().name("ranking").dataType(DataType.INT).order(2).build(),
      Column.builder().name("single").dataType(DataType.BOOLEAN).order(3).build()
    )
    val (topicKey, connectorKey) = setupConnector(props, newSchema)

    try {
      checkFileCount(0, 1, 0)

      val records = pollData(topicKey)
      records.size shouldBe data.length
      val row0 = records.head.key.get
      row0.size shouldBe 3
      row0.cell(0) shouldBe rows.head.cell(0)
      row0.cell(1) shouldBe rows.head.cell(1)
      row0.cell(2) shouldBe rows.head.cell(2)
      val row1 = records(1).key.get
      row1.size shouldBe 3
      row1.cell(0) shouldBe rows(1).cell(0)
      row1.cell(1) shouldBe rows(1).cell(1)
      row1.cell(2) shouldBe rows(1).cell(2)

    } finally result(workerClient.delete(connectorKey))
  }

  @Test
  def testNormalCaseWithoutSchema(): Unit = {
    val (topicKey, connectorKey) = setupConnector(props, None)

    try {
      checkFileCount(0, 1, 0)

      val records = pollData(topicKey)
      records.size shouldBe data.length
      val row0 = records.head.key.get
      row0.size shouldBe 3
      // NOTED: without columns all value are converted to string
      row0.cell(0) shouldBe Cell.of(rows.head.cell(0).name, rows.head.cell(0).value.toString)
      row0.cell(1) shouldBe Cell.of(rows.head.cell(1).name, rows.head.cell(1).value.toString)
      row0.cell(2) shouldBe Cell.of(rows.head.cell(2).name, rows.head.cell(2).value.toString)
      val row1 = records(1).key.get
      row1.size shouldBe 3
      row1.cell(0) shouldBe Cell.of(rows(1).cell(0).name, rows(1).cell(0).value.toString)
      row1.cell(1) shouldBe Cell.of(rows(1).cell(1).name, rows(1).cell(1).value.toString)
      row1.cell(2) shouldBe Cell.of(rows(1).cell(2).name, rows(1).cell(2).value.toString)

    } finally result(workerClient.delete(connectorKey))
  }

  @Test
  def testNormalCaseWithoutEncode(): Unit = {
    // will use default UTF-8
    val newProps = props - FILE_ENCODE_KEY
    val (topicKey, connectorKey) = setupConnector(newProps, schema)

    try {
      checkFileCount(0, 1, 0)

      val records = pollData(topicKey)
      records.size shouldBe data.length
      val row0 = records.head.key.get
      row0.size shouldBe 3
      row0.cell(0) shouldBe rows.head.cell(0)
      row0.cell(1) shouldBe rows.head.cell(1)
      row0.cell(2) shouldBe rows.head.cell(2)
      val row1 = records(1).key.get
      row1.size shouldBe 3
      row1.cell(0) shouldBe rows(1).cell(0)
      row1.cell(1) shouldBe rows(1).cell(1)
      row1.cell(2) shouldBe rows(1).cell(2)

    } finally result(workerClient.delete(connectorKey))
  }

  @Test
  def testPartialColumns(): Unit = {
    // skip last column
    val newSchema = schema.slice(0, schema.length - 1)
    val (topicKey, connectorKey) = setupConnector(props, newSchema)

    try {
      checkFileCount(0, 1, 0)

      val records = pollData(topicKey)
      records.size shouldBe data.length
      val row0 = records.head.key.get
      row0.size shouldBe 2
      row0.cell(0) shouldBe rows.head.cell(0)
      row0.cell(1) shouldBe rows.head.cell(1)
      val row1 = records(1).key.get
      row1.size shouldBe 2
      row1.cell(0) shouldBe rows(1).cell(0)
      row1.cell(1) shouldBe rows(1).cell(1)
    } finally result(workerClient.delete(connectorKey))
  }

  @Test
  def testUnmatchedSchema(): Unit = {
    // the name can't be casted to int
    val newSchema = Seq(Column.builder().name("name").dataType(DataType.INT).order(1).build())
    val (topicKey, connectorKey) = setupConnector(props, newSchema)

    try {
      checkFileCount(0, 0, 1)

      val records = pollData(topicKey, 10 second)
      records.size shouldBe 0

      // add a file to input again
      setupInput()
      checkFileCount(0, 0, 2)
    } finally result(workerClient.delete(connectorKey))
  }

  @Test
  def testInvalidInput(): Unit = {
    val newProps = props ++ Map(INPUT_FOLDER_KEY -> "/abc")
    val (_, connectorKey) = createConnector(newProps, schema)

    ConnectorTestUtils.assertFailedConnector(testUtil, connectorKey)
  }

  @Test
  def testInvalidSchema(): Unit = {
    val newSchema = Seq(
      // 0 is invalid
      Column.builder().name("name").dataType(DataType.STRING).order(0).build(),
      Column.builder().name("ranking").dataType(DataType.INT).order(2).build(),
      Column.builder().name("single").dataType(DataType.BOOLEAN).order(3).build()
    )
    val (_, connectorKey) = createConnector(props, newSchema)

    ConnectorTestUtils.assertFailedConnector(testUtil, connectorKey)
  }

  @Test
  def inputFilesShouldBeRemovedIfCompletedFolderIsNotDefined(): Unit = {
    val newProps = props - COMPLETED_FOLDER_KEY
    val (topicKey, connectorKey) = setupConnector(newProps, schema)

    try {
      CommonUtils.await(
        () => {
          fileSystem.listFileNames(inputDir).asScala.isEmpty
        },
        Duration.ofSeconds(20)
      )
      val records = pollData(topicKey)
      records.size shouldBe data.length
      val row0 = records.head.key.get
      row0.size shouldBe 3
      row0.cell(0) shouldBe rows.head.cell(0)
      row0.cell(1) shouldBe rows.head.cell(1)
      row0.cell(2) shouldBe rows.head.cell(2)
      val row1 = records(1).key.get
      row1.size shouldBe 3
      row1.cell(0) shouldBe rows(1).cell(0)
      row1.cell(1) shouldBe rows(1).cell(1)
      row1.cell(2) shouldBe rows(1).cell(2)

    } finally result(workerClient.delete(connectorKey))
  }

  @After
  def tearDown(): Unit = Releasable.close(fileSystem)
}
