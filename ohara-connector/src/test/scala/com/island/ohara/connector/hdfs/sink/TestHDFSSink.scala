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

package com.island.ohara.connector.hdfs.sink

import java.io.{BufferedReader, InputStream, InputStreamReader}
import java.time.Duration
import java.util.concurrent.TimeUnit

import com.island.ohara.client.filesystem.FileSystem
import com.island.ohara.client.kafka.WorkerClient
import com.island.ohara.common.data.{Cell, DataType, Row, Serializer, _}
import com.island.ohara.common.setting.{ConnectorKey, TopicKey}
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.kafka.connector.json.ConnectorFormatter
import com.island.ohara.kafka.connector.{RowSinkTask, TaskSetting}
import com.island.ohara.kafka.{BrokerClient, Consumer, Producer}
import com.island.ohara.testing.With3Brokers3Workers
import org.junit.{Before, BeforeClass, Test}
import org.scalatest.Matchers

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object TestHDFSSink extends With3Brokers3Workers with Matchers {
  private val TOPIC = TopicKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))

  private val data = Row.of(Cell.of("cf0", 10), Cell.of("cf1", 11))
  private val dataCount = 100

  @BeforeClass
  def init(): Unit = {
    testUtil().hdfs()
    this.setupData(TOPIC.topicNameOnKafka())
  }

  def setupData(topicName: String): Unit = {
    val client = BrokerClient.of(testUtil.brokersConnProps)
    try {
      if (client.exist(topicName)) client.deleteTopic(topicName)
      client.topicCreator().numberOfPartitions(1).numberOfReplications(1).compacted().topicName(topicName).create()
    } finally client.close()

    val producer = Producer
      .builder()
      .connectionProps(testUtil.brokersConnProps)
      .keySerializer(Serializer.ROW)
      .valueSerializer(Serializer.BYTES)
      .build()

    try {
      0 until dataCount foreach (count => {
        val data = Row.of(Cell.of("a", s"${count}a"), Cell.of("b", s"${count}b"))
        producer.sender().key(data).topicName(topicName).send()
      })
      producer.flush()
    } finally producer.close()

    val consumer = Consumer
      .builder()
      .topicName(topicName)
      .offsetFromBegin()
      .connectionProps(testUtil.brokersConnProps)
      .keySerializer(Serializer.ROW)
      .valueSerializer(Serializer.BYTES)
      .build()
    try {
      val records = consumer.poll(java.time.Duration.ofSeconds(60), dataCount)
      val row = records.get(0).key().get
      row.size shouldBe data.size
      row.cell("a").value shouldBe "0a"
      row.cell("b").value shouldBe "0b"
    } finally consumer.close()
  }
}

class TestHDFSSink extends With3Brokers3Workers with Matchers {
  private[this] val workerClient = WorkerClient(testUtil.workersConnProps)

  private[this] val TOPIC = TestHDFSSink.TOPIC
  private[this] val dataCount = TestHDFSSink.dataCount

  private[this] val schema: Seq[Column] = Seq(
    Column.builder().name("a").dataType(DataType.STRING).order(1).build(),
    Column.builder().name("b").dataType(DataType.STRING).order(2).build()
  )

  private[this] val fileSystem = FileSystem.hdfsBuilder.url(testUtil.hdfs.hdfsURL).build

  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)

  private[this] def hdfsTmpDirectory(): String = testUtil.hdfs.tmpDirectory

  private[this] def localHdfsURL(): String = s"file://${hdfsTmpDirectory}"

  private[this] def randomDir(): String = s"${hdfsTmpDirectory}/${CommonUtils.randomString(10)}"

  private[this] def commitDir(topicsDir: String, topicName: String): String = {
    s"$topicsDir/$topicName/partition0"
  }

  private[this] def createConnector(topicsDir: String,
                                    flushCount: Int,
                                    rotateIntervalMs: Long,
                                    needHeader: Boolean,
                                    schema: Seq[Column] = schema): Unit = {
    val connectorKey = ConnectorKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    val topicKey = TOPIC
    result(
      workerClient
        .connectorCreator()
        .connectorClass(classOf[HDFSSink])
        .connectorKey(connectorKey)
        .topicKey(topicKey)
        .numberOfTasks(1)
        .settings(Map(
          HDFS_URL_CONFIG -> localHdfsURL,
          TOPICS_DIR_CONFIG -> topicsDir,
          FLUSH_SIZE_CONFIG -> flushCount.toString,
          ROTATE_INTERVAL_MS_CONFIG -> rotateIntervalMs.toString,
          FILE_NEED_HEADER_CONFIG -> needHeader.toString
        ))
        .columns(schema)
        .create)
  }

  // Verify the commit file is exist.
  private[this] def verifyFileExist(filePath: String): Unit = {
    CommonUtils.await(() => fileSystem.exists(filePath), Duration.ofSeconds(20))
  }

  // Verify the size of commit files in the dir.
  private[this] def verifyCommittedFileSize(dirPath: String, expectedSize: Int): Unit = {
    CommonUtils.await(
      () => {
        if (fileSystem.exists(dirPath))
          fileSystem.listFileNames(dirPath, (fileName: String) => !fileName.contains("_tmp")).size == expectedSize
        else false
      },
      Duration.ofSeconds(20)
    )
  }

  // Verify the size of temp files in the dir.
  private[this] def verifyTemporaryFileSize(dirPath: String, expectedSize: Int): Unit = {
    CommonUtils.await(
      () => {
        if (fileSystem.exists(dirPath))
          fileSystem.listFileNames(dirPath, (fileName: String) => fileName.contains("_tmp")).size == expectedSize
        else false
      },
      Duration.ofSeconds(20)
    )
  }

  //
  private[this] def verifyDataCount(filePath: String, expectedCount: Int): Unit = {
    val file: InputStream = fileSystem.open(filePath)
    val streamReader: InputStreamReader = new InputStreamReader(file)
    val bufferedReaderStream: BufferedReader = new BufferedReader(streamReader)
    try {
      val rowDataCount = Stream.continually(bufferedReaderStream.readLine()).takeWhile(_ != null).toList.size
      rowDataCount shouldBe expectedCount
    } finally {
      bufferedReaderStream.close()
      streamReader.close()
      file.close()
    }
  }

  //
  private[this] def verifyDataHeadLine(filePath: String, expectedString: String): Unit = {
    val file: InputStream = fileSystem.open(filePath)
    val streamReader: InputStreamReader = new InputStreamReader(file)
    val bufferedReaderStream: BufferedReader = new BufferedReader(streamReader)
    try {
      val rowString = Stream.continually(bufferedReaderStream.readLine()).takeWhile(_ != null).head
      rowString shouldBe expectedString
    } finally {
      bufferedReaderStream.close()
      streamReader.close()
      file.close()
    }
  }

  @Before
  def setup(): Unit = {}

  @Test
  def testTaskConfigs(): Unit = {
    val topicKey = TOPIC
    val topicsDir = randomDir()
    val flushCount = 10
    val hdfsURL: String = "hdfs://host1:9000"
    val maxTasks = 5

    val hdfsSink = new HDFSSink()
    hdfsSink.start(
      ConnectorFormatter
        .of()
        .topicKey(topicKey)
        .setting(HDFS_URL_CONFIG, hdfsURL)
        .setting(TOPICS_DIR_CONFIG, topicsDir)
        .setting(FLUSH_SIZE_CONFIG, flushCount.toString)
        .raw())
    val result = hdfsSink._taskSettings(maxTasks)

    result.size shouldBe maxTasks
    result.asScala.foreach(r => {
      r.stringValue(HDFS_URL_CONFIG) shouldBe hdfsURL
      r.stringValue(TOPICS_DIR_CONFIG) shouldBe topicsDir
      r.intValue(FLUSH_SIZE_CONFIG) shouldBe flushCount
    })
  }

  @Test
  def testNormalCase(): Unit = {
    val topicName = TOPIC.topicNameOnKafka()
    val topicsDir = randomDir()
    val flushCount = 10
    val rotateIntervalMs = 0 // don't auto rotate
    val needHeader = false

    // Creates a new connect and start.
    createConnector(topicsDir, flushCount, rotateIntervalMs, needHeader)

    val committedDir = commitDir(topicsDir, topicName)
    verifyCommittedFileSize(committedDir, dataCount / flushCount)
    verifyFileExist(s"$committedDir/$topicName-0-000000090.csv")
    verifyDataCount(s"$committedDir/$topicName-0-000000090.csv", flushCount)
    verifyDataHeadLine(s"$committedDir/$topicName-0-000000000.csv", "0a,0b")
    verifyDataHeadLine(s"$committedDir/$topicName-0-000000090.csv", "90a,90b")
    verifyTemporaryFileSize(committedDir, 0)
  }

  @Test
  def testNormalCaseWithHeader(): Unit = {
    val topicName = TOPIC.topicNameOnKafka()
    val topicsDir = randomDir()
    val flushCount = 10
    val rotateIntervalMs = 0 // don't auto rotate
    val needHeader = true

    // Creates a new connect and start.
    createConnector(topicsDir, flushCount, rotateIntervalMs, needHeader)

    val committedDir = commitDir(topicsDir, topicName)
    verifyCommittedFileSize(committedDir, dataCount / flushCount)
    verifyTemporaryFileSize(committedDir, 0)
    verifyFileExist(s"$committedDir/$topicName-0-000000090.csv")
    verifyDataCount(s"$committedDir/$topicName-0-000000090.csv", flushCount + 1)
    verifyDataHeadLine(s"$committedDir/$topicName-0-000000000.csv", "a,b")
    verifyDataHeadLine(s"$committedDir/$topicName-0-000000090.csv", "a,b")
  }

  @Test
  def testFlushSizeOne(): Unit = {
    val topicName = TOPIC.topicNameOnKafka()
    val topicsDir = randomDir()
    val flushCount = 1
    val rotateIntervalMs = 0 // don't auto rotate
    val needHeader = false

    // Creates a new connect and start.
    createConnector(topicsDir, flushCount, rotateIntervalMs, needHeader)

    val committedDir = commitDir(topicsDir, topicName)
    verifyCommittedFileSize(committedDir, dataCount / flushCount)
    verifyTemporaryFileSize(committedDir, 0)
    verifyFileExist(s"$committedDir/$topicName-0-000000000.csv")
    verifyFileExist(s"$committedDir/$topicName-0-000000001.csv")
    verifyFileExist(s"$committedDir/$topicName-0-000000099.csv")
    verifyDataCount(s"$committedDir/$topicName-0-000000000.csv", flushCount)
    verifyDataCount(s"$committedDir/$topicName-0-000000099.csv", flushCount)
  }

  @Test
  def testRotateOnTime(): Unit = {
    val topicName = TOPIC.topicNameOnKafka()
    val topicsDir = randomDir()
    val flushCount = 999999
    val rotateIntervalMs = 10 * 1000 // auto rotate per 10s
    val needHeader = false

    // Creates a new connect and start.
    createConnector(topicsDir, flushCount, rotateIntervalMs, needHeader)

    val committedDir = commitDir(topicsDir, topicName)
    verifyCommittedFileSize(committedDir, 1)
    verifyTemporaryFileSize(committedDir, 0)
    verifyFileExist(s"$committedDir/$topicName-0-000000000.csv")
    verifyDataCount(s"$committedDir/$topicName-0-000000000.csv", dataCount)
  }

  @Test
  def testDataNotMappingSchema(): Unit = {
    val topicName = TOPIC.topicNameOnKafka()
    val topicsDir = randomDir()
    val flushCount = 10
    val rotateIntervalMs = 0 // don't auto rotate
    val needHeader = false
    val schema = Seq(Column.builder().name("cccc").dataType(DataType.BOOLEAN).order(1).build())

    // Creates a new connect and start.
    createConnector(topicsDir, flushCount, rotateIntervalMs, needHeader, schema)

    TimeUnit.SECONDS.sleep(10)

    val committedDir = commitDir(topicsDir, topicName)
    CommonUtils.await(() => fileSystem.exists(committedDir) == false, Duration.ofSeconds(20))
  }

  @Test
  def testRunMiniClusterAndAssignConfig(): Unit = {
    val connectorKey = ConnectorKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    val topicKey = TOPIC
    val topicsDir = randomDir()
    val flushCount = 2000
    val rotateIntervalMs = 60000
    val needHeader = false

    // Creates a new connect and start.
    result(
      workerClient
        .connectorCreator()
        .connectorClass(classOf[SimpleHDFSSink])
        .connectorKey(connectorKey)
        .topicKey(topicKey)
        .numberOfTasks(1)
        .settings(Map(
          HDFS_URL_CONFIG -> localHdfsURL,
          TOPICS_DIR_CONFIG -> topicsDir,
          FLUSH_SIZE_CONFIG -> flushCount.toString,
          ROTATE_INTERVAL_MS_CONFIG -> rotateIntervalMs.toString,
          FILE_NEED_HEADER_CONFIG -> needHeader.toString
        ))
        .create)

    CommonUtils
      .await(() => SimpleHDFSSinkTask.setting != null && SimpleHDFSSinkTask.props != null, Duration.ofSeconds(20))
    CommonUtils
      .await(() => SimpleHDFSSinkTask.setting.stringValue(TOPICS_DIR_CONFIG) == topicsDir, Duration.ofSeconds(20))
    CommonUtils
      .await(() => SimpleHDFSSinkTask.setting.intValue(FLUSH_SIZE_CONFIG) == flushCount, Duration.ofSeconds(20))
    CommonUtils.await(() => SimpleHDFSSinkTask.setting.longValue(ROTATE_INTERVAL_MS_CONFIG) == rotateIntervalMs,
                      Duration.ofSeconds(20))
    CommonUtils.await(() => SimpleHDFSSinkTask.setting.booleanValue(FILE_NEED_HEADER_CONFIG) == needHeader,
                      Duration.ofSeconds(20))
    CommonUtils.await(() => SimpleHDFSSinkTask.setting.stringValue(FILE_ENCODE_CONFIG) == FILE_ENCODE_DEFAULT,
                      Duration.ofSeconds(20))
    CommonUtils.await(() => SimpleHDFSSinkTask.props.hdfsURL == localHdfsURL, Duration.ofSeconds(20))
  }
}

class SimpleHDFSSink extends HDFSSink {
  override protected def _taskClass(): Class[_ <: RowSinkTask] = {
    classOf[SimpleHDFSSinkTask]
  }
}

class SimpleHDFSSinkTask extends HDFSSinkTask {
  override def _start(setting: TaskSetting): Unit = {
    super._start(setting)
    SimpleHDFSSinkTask.setting = setting
    SimpleHDFSSinkTask.props = hdfsSinkProps
  }
}

object SimpleHDFSSinkTask {
  @volatile var setting: TaskSetting = _
  @volatile var props: HDFSSinkProps = _
}
