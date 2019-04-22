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

package com.island.ohara.connector.hdfs

import java.io.{BufferedReader, InputStream, InputStreamReader}
import java.time.Duration
import java.util.concurrent.{ConcurrentHashMap, TimeUnit}

import com.island.ohara.client.kafka.WorkerClient
import com.island.ohara.common.data.{Cell, DataType, Row, Serializer, _}
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.connector.hdfs.creator.LocalHDFSStorageCreator
import com.island.ohara.connector.hdfs.storage.HDFSStorage
import com.island.ohara.kafka.Producer
import com.island.ohara.kafka.connector.json.ConnectorFormatter
import com.island.ohara.kafka.connector.{RowSinkTask, TaskConfig}
import com.island.ohara.testing.With3Brokers3Workers
import org.apache.hadoop.fs.Path
import org.junit.Test
import org.scalatest.Matchers

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
class TestHDFSSinkConnector extends With3Brokers3Workers with Matchers {
  private[this] val workerClient = WorkerClient(testUtil.workersConnProps)
  private[this] val hdfsURL: String = "hdfs://host1:9000"
  private[this] val tmpDir: String = "/tmp"

  private[this] val schema = Seq(Column.builder().name("cf0").dataType(DataType.BOOLEAN).order(1).build())

  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)

  @Test
  def testTaskConfigs(): Unit = {
    val maxTasks = 5
    val hdfsSinkConnector = new HDFSSinkConnector()

    hdfsSinkConnector.start(
      ConnectorFormatter.of().id("test").topicName("topic").setting(HDFS_URL, hdfsURL).setting(TMP_DIR, tmpDir).raw())
    val result = hdfsSinkConnector._taskConfigs(maxTasks)

    result.size shouldBe maxTasks
    result.asScala.foreach(r => {
      r.raw().get(HDFS_URL) shouldBe hdfsURL
      r.raw().get(TMP_DIR) shouldBe tmpDir
    })
  }

  @Test
  def testRunMiniClusterAndAssignConfig(): Unit = {
    val connectorName = methodName
    val topicName = methodName
    val sinkTasks = 2
    val flushLineCountName = FLUSH_LINE_COUNT
    val flushLineCount = "2000"
    val rotateIntervalMSName = ROTATE_INTERVAL_MS
    val tmpDirName = TMP_DIR
    val tmpDirPath = "/home/tmp"
    val hdfsURLName = HDFS_URL

    val localURL = s"file://${testUtil.hdfs.tmpDirectory}"
    result(
      workerClient
        .connectorCreator()
        .id(connectorName)
        .connectorClass(classOf[SimpleHDFSSinkConnector])
        .topicName(topicName)
        .numberOfTasks(sinkTasks)
        .settings(Map(flushLineCountName -> flushLineCount, tmpDirName -> tmpDirPath, hdfsURLName -> localURL))
        .columns(schema)
        .create)

    CommonUtils
      .await(() => SimpleHDFSSinkTask.taskProps.get(flushLineCountName) == flushLineCount, Duration.ofSeconds(20))
    CommonUtils.await(() => SimpleHDFSSinkTask.taskProps.get(rotateIntervalMSName) == null, Duration.ofSeconds(20))
    CommonUtils.await(() => SimpleHDFSSinkTask.taskProps.get(tmpDirName) == tmpDirPath, Duration.ofSeconds(10))
    CommonUtils.await(() => SimpleHDFSSinkTask.sinkConnectorConfig.dataDir == HDFSSinkConnectorConfig.DATA_DIR_DEFAULT,
                      Duration.ofSeconds(20))
    CommonUtils.await(() => SimpleHDFSSinkTask.sinkConnectorConfig.flushLineCount == 2000, Duration.ofSeconds(20))
  }

  @Test
  def testHDFSSinkConnector(): Unit = {
    val sinkTasks = 1
    val flushLineCountName = FLUSH_LINE_COUNT
    val flushLineCount = "10"
    val tmpDirName = TMP_DIR
    val dataDirName = DATA_DIR
    val isHeader = DATAFILE_NEEDHEADER
    val hdfsURLName = HDFS_URL
    val connectorName = methodName
    val topicName = methodName
    val rowCount = 100
    val row = Row.of(Cell.of("cf0", 10), Cell.of("cf1", 11))
    val hdfsCreatorClassName = HDFS_STORAGE_CREATOR_CLASS
    val hdfsCreatorClassNameValue = classOf[LocalHDFSStorageCreator].getName

    val fileSystem = testUtil.hdfs.fileSystem()
    val storage = new HDFSStorage(fileSystem)
    val tmpDirPath = s"${testUtil.hdfs.tmpDirectory}/${CommonUtils.randomString(10)}"
    val dataDirPath = s"${testUtil.hdfs.tmpDirectory}/${CommonUtils.randomString(10)}"
    val producer = Producer
      .builder[Row, Array[Byte]]()
      .connectionProps(testUtil.brokersConnProps)
      .keySerializer(Serializer.ROW)
      .valueSerializer(Serializer.BYTES)
      .build()
    try {
      0 until rowCount foreach (_ => producer.sender().key(row).topicName(topicName).send())
      producer.flush()
    } finally producer.close()

    val localURL = s"file://${testUtil.hdfs.tmpDirectory}"
    result(
      workerClient
        .connectorCreator()
        .id(connectorName)
        .connectorClass(classOf[HDFSSinkConnector])
        .topicName(topicName)
        .numberOfTasks(sinkTasks)
        .settings(Map(
          flushLineCountName -> flushLineCount,
          tmpDirName -> tmpDirPath,
          hdfsURLName -> localURL,
          hdfsCreatorClassName -> hdfsCreatorClassNameValue,
          dataDirName -> dataDirPath,
          isHeader -> "false"
        ))
        .columns(schema)
        .create)

    TimeUnit.SECONDS.sleep(5)
    val partitionID: String = "partition0"
    CommonUtils.await(() => storage.list(s"$dataDirPath/$topicName/$partitionID").size == 10, Duration.ofSeconds(20))

    CommonUtils.await(
      () =>
        FileUtils.getStopOffset(storage.list(s"$dataDirPath/$topicName/$partitionID").map(FileUtils.fileName)) == 100,
      Duration.ofSeconds(20))

    CommonUtils.await(() =>
                        storage
                          .list(s"$dataDirPath/$topicName/$partitionID")
                          .map(FileUtils.fileName)
                          .contains("part-000000090-000000100.csv"),
                      Duration.ofSeconds(20))

    val path: Path = new Path(s"$dataDirPath/$topicName/$partitionID/part-000000090-000000100.csv")
    val file: InputStream = testUtil.hdfs.fileSystem.open(path)
    val streamReader: InputStreamReader = new InputStreamReader(file)
    val bufferedReaderStream: BufferedReader = new BufferedReader(streamReader)
    try {
      val rowDataCount = Stream.continually(bufferedReaderStream.readLine()).takeWhile(_ != null).toList.size
      rowDataCount shouldBe 10
    } finally {
      bufferedReaderStream.close()
      streamReader.close()
      file.close()
    }
  }

  @Test
  def testFlushSizeOne(): Unit = {
    val sinkTasks = 1
    val flushLineCountName = FLUSH_LINE_COUNT
    val flushLineCount = "1"
    val tmpDirName = TMP_DIR
    val dataDirName = DATA_DIR
    val isHeader = DATAFILE_NEEDHEADER
    val hdfsURLName = HDFS_URL
    val connectorName = methodName
    val topicName = methodName
    val rowCount = 10
    val row = Row.of(Cell.of("cf0", 10), Cell.of("cf1", 11))
    val hdfsCreatorClassName = HDFS_STORAGE_CREATOR_CLASS
    val hdfsCreatorClassNameValue = classOf[LocalHDFSStorageCreator].getName

    val fileSystem = testUtil.hdfs.fileSystem()
    val storage = new HDFSStorage(fileSystem)
    val tmpDirPath = s"${testUtil.hdfs.tmpDirectory}/tmp"
    val dataDirPath = s"${testUtil.hdfs.tmpDirectory}/data"
    val producer = Producer
      .builder[Row, Array[Byte]]()
      .connectionProps(testUtil.brokersConnProps)
      .keySerializer(Serializer.ROW)
      .valueSerializer(Serializer.BYTES)
      .build()
    try {
      0 until rowCount foreach (_ => producer.sender().key(row).topicName(topicName).send())
      producer.flush()
    } finally producer.close()

    val localURL = s"file://${testUtil.hdfs.tmpDirectory}"
    result(
      workerClient
        .connectorCreator()
        .id(connectorName)
        .connectorClass(classOf[HDFSSinkConnector])
        .topicName(topicName)
        .numberOfTasks(sinkTasks)
        .settings(Map(
          flushLineCountName -> flushLineCount,
          tmpDirName -> tmpDirPath,
          hdfsURLName -> localURL,
          hdfsCreatorClassName -> hdfsCreatorClassNameValue,
          dataDirName -> dataDirPath,
          isHeader -> "false"
        ))
        .columns(schema)
        .create)

    TimeUnit.SECONDS.sleep(5)
    val partitionID: String = "partition0"
    CommonUtils.await(() => storage.list(s"$dataDirPath/$topicName/$partitionID").size == 10, Duration.ofSeconds(30))

    CommonUtils.await(() =>
                        storage
                          .list(s"$dataDirPath/$topicName/$partitionID")
                          .map(FileUtils.fileName)
                          .contains("part-000000000-000000001.csv"),
                      Duration.ofSeconds(20))

    CommonUtils.await(() =>
                        storage
                          .list(s"$dataDirPath/$topicName/$partitionID")
                          .map(FileUtils.fileName)
                          .contains("part-000000001-000000002.csv"),
                      Duration.ofSeconds(20))

    CommonUtils.await(() =>
                        storage
                          .list(s"$dataDirPath/$topicName/$partitionID")
                          .map(FileUtils.fileName)
                          .contains("part-000000008-000000009.csv"),
                      Duration.ofSeconds(20))

    val path: Path = new Path(s"$dataDirPath/$topicName/$partitionID/part-000000005-000000006.csv")
    val file: InputStream = testUtil.hdfs.fileSystem.open(path)
    val streamReader: InputStreamReader = new InputStreamReader(file)
    val bufferedReaderStream: BufferedReader = new BufferedReader(streamReader)
    try {
      val rowDataCount = Stream.continually(bufferedReaderStream.readLine()).takeWhile(_ != null).toList.size
      rowDataCount shouldBe 1
    } finally {
      bufferedReaderStream.close()
      streamReader.close()
      file.close()
    }
  }

  @Test
  def testRecoverOffset(): Unit = {
    val sinkTasks = 1
    val flushLineCountName = FLUSH_LINE_COUNT
    val flushLineCount = "100"
    val tmpDirName = TMP_DIR
    val dataDirName = DATA_DIR
    val hdfsURLName = HDFS_URL
    val needHeader = DATAFILE_NEEDHEADER
    val connectorName = methodName
    val topicName = methodName
    val rowCount = 200
    val row = Row.of(Cell.of("cf0", 10), Cell.of("cf1", 11))
    val hdfsCreatorClassName = HDFS_STORAGE_CREATOR_CLASS
    val hdfsCreatorClassNameValue = classOf[LocalHDFSStorageCreator].getName

    val fileSystem = testUtil.hdfs.fileSystem()
    val storage = new HDFSStorage(fileSystem)
    val tmpDirPath = s"${testUtil.hdfs.tmpDirectory}/${CommonUtils.randomString(10)}"
    val dataDirPath = s"${testUtil.hdfs.tmpDirectory}/${CommonUtils.randomString(10)}"
    //Before running the Kafka Connector, create the file to local hdfs for test recover offset
    val partitionID: String = "partition0"
    fileSystem.createNewFile(new Path(s"$dataDirPath/$topicName/$partitionID/part-000000000-000000099.csv"))

    val producer = Producer
      .builder[Row, Array[Byte]]()
      .connectionProps(testUtil.brokersConnProps)
      .keySerializer(Serializer.ROW)
      .valueSerializer(Serializer.BYTES)
      .build()
    try {
      0 until rowCount foreach (_ => producer.sender().key(row).topicName(topicName).send())
      producer.flush()
    } finally producer.close()

    val localURL = s"file://${testUtil.hdfs.tmpDirectory}"
    result(
      workerClient
        .connectorCreator()
        .id(connectorName)
        .connectorClass(classOf[HDFSSinkConnector])
        .topicName(topicName)
        .numberOfTasks(sinkTasks)
        .settings(Map(
          flushLineCountName -> flushLineCount,
          tmpDirName -> tmpDirPath,
          hdfsURLName -> localURL,
          hdfsCreatorClassName -> hdfsCreatorClassNameValue,
          needHeader -> "true",
          dataDirName -> dataDirPath
        ))
        .columns(schema)
        .create)

    TimeUnit.SECONDS.sleep(5)
    CommonUtils.await(() => storage.list(s"$dataDirPath/$topicName/$partitionID").size == 2, Duration.ofSeconds(20))

    CommonUtils.await(
      () =>
        FileUtils.getStopOffset(storage.list(s"$dataDirPath/$topicName/$partitionID").map(FileUtils.fileName)) == 199,
      Duration.ofSeconds(20))

    CommonUtils.await(() =>
                        storage
                          .list(s"$dataDirPath/$topicName/$partitionID")
                          .map(FileUtils.fileName)
                          .contains("part-000000099-000000199.csv"),
                      Duration.ofSeconds(20))

    val path: Path = new Path(s"$dataDirPath/$topicName/$partitionID/part-000000099-000000199.csv")
    val file: InputStream = testUtil.hdfs.fileSystem.open(path)
    val streamReader: InputStreamReader = new InputStreamReader(file)
    val bufferedReaderStream: BufferedReader = new BufferedReader(streamReader)
    try {
      val rowDataCount = Stream.continually(bufferedReaderStream.readLine()).takeWhile(_ != null).toList.size
      rowDataCount shouldBe 101
    } finally {
      bufferedReaderStream.close()
      streamReader.close()
      file.close()
    }
  }

  @Test
  def testDataNotMappingSchema(): Unit = {

    val sinkTasks = 1
    val flushLineCountName = FLUSH_LINE_COUNT
    val flushLineCount = "10"
    val tmpDirName = TMP_DIR
    val dataDirName = DATA_DIR
    val isHeader = DATAFILE_NEEDHEADER
    val hdfsURLName = HDFS_URL
    val connectorName = methodName
    val topicName = methodName
    val rowCount = 100
    val row = Row.of(Cell.of("cf0", 10), Cell.of("cf1", 11))
    val hdfsCreatorClassName = HDFS_STORAGE_CREATOR_CLASS
    val hdfsCreatorClassNameValue = classOf[LocalHDFSStorageCreator].getName

    val fileSystem = testUtil.hdfs.fileSystem
    val storage = new HDFSStorage(fileSystem)
    val tmpDirPath = s"${testUtil.hdfs.tmpDirectory}/${CommonUtils.randomString(10)}"
    val dataDirPath = s"${testUtil.hdfs.tmpDirectory}/${CommonUtils.randomString(10)}"

    val producer = Producer
      .builder[Row, Array[Byte]]()
      .connectionProps(testUtil.brokersConnProps)
      .keySerializer(Serializer.ROW)
      .valueSerializer(Serializer.BYTES)
      .build()
    try {
      0 until rowCount foreach (_ => producer.sender().key(row).topicName(topicName).send())
      producer.flush()
    } finally producer.close()

    val localURL = s"file://${testUtil.hdfs.tmpDirectory}"
    result(
      workerClient
        .connectorCreator()
        .id(connectorName)
        .connectorClass(classOf[HDFSSinkConnector])
        .topicName(topicName)
        .numberOfTasks(sinkTasks)
        .settings(Map(
          flushLineCountName -> flushLineCount,
          tmpDirName -> tmpDirPath,
          hdfsURLName -> localURL,
          hdfsCreatorClassName -> hdfsCreatorClassNameValue,
          dataDirName -> dataDirPath,
          isHeader -> "false"
        ))
        .columns(Seq(Column.builder().name("cccc").dataType(DataType.BOOLEAN).order(1).build()))
        .create)

    TimeUnit.SECONDS.sleep(5)
    val partitionID: String = "partition0"
    CommonUtils.await(() => storage.list(s"$dataDirPath/$topicName/$partitionID").isEmpty, Duration.ofSeconds(20))
  }

  @Test
  def testStopTask(): Unit = {
    val hdfsSinkTask = new HDFSSinkTask()
    val map: java.util.Map[String, String] = new java.util.HashMap[String, String]()
    val localURL = s"file://${testUtil.hdfs.tmpDirectory}"
    map.put("topics", "topic1")
    map.put("name", "name1")
    map.put(HDFS_URL, localURL)
    hdfsSinkTask.start(map)
    hdfsSinkTask.stop()
  }

  @Test
  def testStopTaskNotNPE(): Unit = {
    val hdfsSinkTask = new HDFSSinkTask()
    hdfsSinkTask.stop()
  }
}

class SimpleHDFSSinkConnector extends HDFSSinkConnector {
  override protected def _taskClass(): Class[_ <: RowSinkTask] = {
    classOf[SimpleHDFSSinkTask]
  }
}

class SimpleHDFSSinkTask extends HDFSSinkTask {
  override def _start(props: TaskConfig): Unit = {
    super._start(props)
    props.raw().asScala.foreach {
      case (k, v) => SimpleHDFSSinkTask.taskProps.put(k, v)
    }
    SimpleHDFSSinkTask.sinkConnectorConfig = hdfsSinkConnectorConfig
  }
}

object SimpleHDFSSinkTask {
  val taskProps = new ConcurrentHashMap[String, String]
  var sinkConnectorConfig: HDFSSinkConnectorConfig = _
}
